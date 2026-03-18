const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const config = require('../config');
const customerService = require('../services/customerService');
const logger = require('../utils/logger');
const rateLimit = require('../middleware/rateLimit');

const router = express.Router();

const authLimiter = rateLimit({ windowMs: 60000, max: 10, message: 'Too many attempts. Please wait a minute.' });
const signupLimiter = rateLimit({ windowMs: 60000, max: 5, message: 'Too many signups. Please wait a minute.' });

// Signup
router.post('/signup', signupLimiter, async (req, res) => {
  try {
    const { email, password, name } = req.body;
    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }

    const existing = await customerService.findByEmail(email);
    if (existing) {
      return res.status(409).json({ error: 'Email already registered' });
    }

    const hash = await bcrypt.hash(password, 10);
    const customer = await customerService.create(email, hash, name);

    const payload = {
      id: customer.id,
      email: customer.email,
      name: customer.name,
      role: 'customer',
    };

    const accessToken = jwt.sign(payload, config.jwtSecret, { expiresIn: '1h' });
    const refreshToken = jwt.sign({ id: customer.id, role: 'customer' }, config.jwtRefreshSecret, { expiresIn: '7d' });

    logger.info({ customerId: customer.id, email }, 'Customer signed up');

    res.status(201).json({
      accessToken,
      refreshToken,
      customer: payload,
    });
  } catch (err) {
    logger.error(err, 'Signup error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Login
router.post('/login', authLimiter, async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }

    const customer = await customerService.findByEmail(email);
    if (!customer) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    if (customer.status === 'suspended') {
      return res.status(403).json({ error: 'Account suspended. Contact support.' });
    }

    const valid = await bcrypt.compare(password, customer.password_hash);
    if (!valid) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const payload = {
      id: customer.id,
      email: customer.email,
      name: customer.name,
      role: 'customer',
    };

    const accessToken = jwt.sign(payload, config.jwtSecret, { expiresIn: '1h' });
    const refreshToken = jwt.sign({ id: customer.id, role: 'customer' }, config.jwtRefreshSecret, { expiresIn: '7d' });

    logger.info({ customerId: customer.id, email }, 'Customer logged in');

    res.json({
      accessToken,
      refreshToken,
      customer: payload,
    });
  } catch (err) {
    logger.error(err, 'Login error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Refresh
router.post('/refresh', async (req, res) => {
  try {
    const { refreshToken } = req.body;
    if (!refreshToken) {
      return res.status(400).json({ error: 'Refresh token is required' });
    }

    const decoded = jwt.verify(refreshToken, config.jwtRefreshSecret);
    if (decoded.role !== 'customer') {
      return res.status(401).json({ error: 'Invalid token' });
    }

    const customer = await customerService.findById(decoded.id);
    if (!customer) {
      return res.status(401).json({ error: 'Customer not found' });
    }

    if (customer.status === 'suspended') {
      return res.status(403).json({ error: 'Account suspended' });
    }

    const payload = {
      id: customer.id,
      email: customer.email,
      name: customer.name,
      role: 'customer',
    };

    const newAccessToken = jwt.sign(payload, config.jwtSecret, { expiresIn: '1h' });
    const newRefreshToken = jwt.sign({ id: customer.id, role: 'customer' }, config.jwtRefreshSecret, { expiresIn: '7d' });

    res.json({ accessToken: newAccessToken, refreshToken: newRefreshToken });
  } catch (err) {
    return res.status(401).json({ error: 'Invalid refresh token' });
  }
});

// Get current customer profile
router.get('/me', async (req, res) => {
  const header = req.headers.authorization;
  if (!header || !header.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Unauthorized' });
  }
  try {
    const payload = jwt.verify(header.slice(7), config.jwtSecret);
    if (payload.role !== 'customer') {
      return res.status(401).json({ error: 'Not a customer token' });
    }
    const customer = await customerService.findById(payload.id);
    if (!customer) return res.status(404).json({ error: 'Not found' });

    res.json({
      id: customer.id,
      email: customer.email,
      name: customer.name,
      status: customer.status,
      package: customer.package,
      createdAt: customer.created_at,
    });
  } catch {
    return res.status(401).json({ error: 'Invalid token' });
  }
});

module.exports = router;
