const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const config = require('../config');
const agentService = require('../services/agentService');
const authMiddleware = require('../middleware/auth');
const logger = require('../utils/logger');

const router = express.Router();

router.post('/login', async (req, res) => {
  try {
    const { username, password } = req.body;
    if (!username || !password) {
      return res.status(400).json({ error: 'Username and password are required' });
    }

    const agent = await agentService.findByUsername(username);
    if (!agent) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const valid = await bcrypt.compare(password, agent.password_hash);
    if (!valid) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const payload = {
      id: agent.id,
      username: agent.username,
      displayName: agent.display_name,
      twilioIdentity: agent.twilio_identity,
      twilioPhoneNumber: agent.twilio_phone_number,
    };

    const accessToken = jwt.sign(payload, config.jwtSecret, { expiresIn: '1h' });
    const refreshToken = jwt.sign({ id: agent.id }, config.jwtRefreshSecret, { expiresIn: '7d' });

    // Set agent as available on login
    await agentService.updateStatus(agent.id, 'available');

    logger.info({ agentId: agent.id, username }, 'Agent logged in');

    res.json({
      accessToken,
      refreshToken,
      agent: payload,
    });
  } catch (err) {
    logger.error(err, 'Login error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

router.post('/refresh', async (req, res) => {
  try {
    const { refreshToken } = req.body;
    if (!refreshToken) {
      return res.status(400).json({ error: 'Refresh token is required' });
    }

    const decoded = jwt.verify(refreshToken, config.jwtRefreshSecret);
    const agent = await agentService.findById(decoded.id);
    if (!agent) {
      return res.status(401).json({ error: 'Agent not found' });
    }

    const payload = {
      id: agent.id,
      username: agent.username,
      displayName: agent.display_name,
      twilioIdentity: agent.twilio_identity,
      twilioPhoneNumber: agent.twilio_phone_number,
    };

    const newAccessToken = jwt.sign(payload, config.jwtSecret, { expiresIn: '1h' });
    const newRefreshToken = jwt.sign({ id: agent.id }, config.jwtRefreshSecret, { expiresIn: '7d' });

    res.json({ accessToken: newAccessToken, refreshToken: newRefreshToken });
  } catch (err) {
    return res.status(401).json({ error: 'Invalid refresh token' });
  }
});

router.post('/logout', authMiddleware, async (req, res) => {
  try {
    await agentService.updateStatus(req.agent.id, 'offline');
    logger.info({ agentId: req.agent.id }, 'Agent logged out');
    res.json({ ok: true });
  } catch (err) {
    logger.error(err, 'Logout error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
