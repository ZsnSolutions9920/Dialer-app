const express = require('express');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const config = require('../config');
const pool = require('../db/pool');
const customerService = require('../services/customerService');
const logger = require('../utils/logger');
const rateLimit = require('../middleware/rateLimit');

const router = express.Router();

const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD || 'admin123';
const adminLoginLimiter = rateLimit({ windowMs: 60000, max: 5, message: 'Too many login attempts.' });

// Admin auth middleware
function adminAuth(req, res, next) {
  const header = req.headers.authorization;
  if (!header || !header.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Unauthorized' });
  }
  try {
    const payload = jwt.verify(header.slice(7), config.jwtSecret);
    if (payload.role !== 'admin') {
      return res.status(403).json({ error: 'Admin access required' });
    }
    req.admin = payload;
    next();
  } catch {
    return res.status(401).json({ error: 'Invalid token' });
  }
}

// Admin login
router.post('/login', adminLoginLimiter, (req, res) => {
  const { password } = req.body;
  if (!password || password !== ADMIN_PASSWORD) {
    return res.status(401).json({ error: 'Invalid admin password' });
  }

  const token = jwt.sign({ role: 'admin' }, config.jwtSecret, { expiresIn: '24h' });
  logger.info('Admin logged in');
  res.json({ token });
});

// Dashboard stats
router.get('/stats', adminAuth, async (req, res) => {
  try {
    const stats = await customerService.getStats();
    res.json(stats);
  } catch (err) {
    logger.error(err, 'Admin stats error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// List customers
router.get('/customers', adminAuth, async (req, res) => {
  try {
    const { page = 1, limit = 50, search } = req.query;
    const result = await customerService.getAll({ page: parseInt(page), limit: parseInt(limit), search });
    res.json(result);
  } catch (err) {
    logger.error(err, 'Admin list customers error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Update customer status
router.patch('/customers/:id/status', adminAuth, async (req, res) => {
  try {
    const { status } = req.body;
    if (!['active', 'suspended'].includes(status)) {
      return res.status(400).json({ error: 'Invalid status' });
    }
    const customer = await customerService.updateStatus(req.params.id, status);
    if (!customer) return res.status(404).json({ error: 'Customer not found' });
    logger.info({ customerId: req.params.id, status }, 'Admin updated customer status');
    res.json(customer);
  } catch (err) {
    logger.error(err, 'Admin update status error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Update customer package
router.patch('/customers/:id/package', adminAuth, async (req, res) => {
  try {
    const { package: pkg } = req.body;
    if (!['free', 'basic', 'silver', 'premium'].includes(pkg)) {
      return res.status(400).json({ error: 'Invalid package' });
    }
    const customer = await customerService.updatePackage(req.params.id, pkg);
    if (!customer) return res.status(404).json({ error: 'Customer not found' });
    logger.info({ customerId: req.params.id, package: pkg }, 'Admin updated customer package');
    res.json(customer);
  } catch (err) {
    logger.error(err, 'Admin update package error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Delete customer
router.delete('/customers/:id', adminAuth, async (req, res) => {
  try {
    await customerService.deleteCustomer(req.params.id);
    logger.info({ customerId: req.params.id }, 'Admin deleted customer');
    res.json({ ok: true });
  } catch (err) {
    logger.error(err, 'Admin delete customer error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// ─── MINUTES ASSIGNMENTS ───

// Assign minutes to customer
router.post('/customers/:id/minutes', adminAuth, async (req, res) => {
  try {
    const { minutes, packageId, pricePaid, notes } = req.body;
    if (!minutes || minutes <= 0) {
      return res.status(400).json({ error: 'Invalid minutes value' });
    }
    const { rows } = await pool.query(
      `INSERT INTO customer_minutes (customer_id, minutes_total, package_id, price_paid, notes)
       VALUES ($1, $2, $3, $4, $5) RETURNING *`,
      [req.params.id, minutes, packageId || null, pricePaid || 0, notes || null]
    );
    logger.info({ customerId: req.params.id, minutes }, 'Admin assigned minutes');
    res.status(201).json(rows[0]);
  } catch (err) {
    logger.error(err, 'Admin assign minutes error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Get customer's minutes summary
router.get('/customers/:id/minutes', adminAuth, async (req, res) => {
  try {
    const { rows } = await pool.query(
      `SELECT * FROM customer_minutes WHERE customer_id = $1 ORDER BY assigned_at DESC`,
      [req.params.id]
    );
    const summary = await pool.query(
      `SELECT COALESCE(SUM(minutes_total), 0) AS total_minutes,
              COALESCE(SUM(minutes_used), 0) AS used_minutes,
              COALESCE(SUM(minutes_total) - SUM(minutes_used), 0) AS remaining_minutes
       FROM customer_minutes WHERE customer_id = $1`,
      [req.params.id]
    );
    res.json({ assignments: rows, summary: summary.rows[0] });
  } catch (err) {
    logger.error(err, 'Admin get minutes error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// ─── PHONE NUMBER ASSIGNMENTS ───

// Assign phone number to customer
router.post('/customers/:id/numbers', adminAuth, async (req, res) => {
  try {
    const { phoneNumber, friendlyName, type, monthlyPrice, notes } = req.body;
    if (!phoneNumber) {
      return res.status(400).json({ error: 'Phone number is required' });
    }
    // Check if number already assigned
    const existing = await pool.query(
      'SELECT id FROM customer_numbers WHERE phone_number = $1 AND status = $2',
      [phoneNumber, 'active']
    );
    if (existing.rows.length > 0) {
      return res.status(409).json({ error: 'Number already assigned to a customer' });
    }
    const { rows } = await pool.query(
      `INSERT INTO customer_numbers (customer_id, phone_number, friendly_name, type, monthly_price, notes)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
      [req.params.id, phoneNumber, friendlyName || null, type || 'local', monthlyPrice || 0, notes || null]
    );
    logger.info({ customerId: req.params.id, phoneNumber }, 'Admin assigned phone number');
    res.status(201).json(rows[0]);
  } catch (err) {
    logger.error(err, 'Admin assign number error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Get customer's assigned numbers
router.get('/customers/:id/numbers', adminAuth, async (req, res) => {
  try {
    const { rows } = await pool.query(
      'SELECT * FROM customer_numbers WHERE customer_id = $1 ORDER BY assigned_at DESC',
      [req.params.id]
    );
    res.json({ numbers: rows });
  } catch (err) {
    logger.error(err, 'Admin get numbers error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Helper: detach number from agent if it's the active one
async function detachNumberFromAgent(number) {
  const agentResult = await pool.query(
    'SELECT id, twilio_phone_number FROM agents WHERE customer_id = $1',
    [number.customer_id]
  );
  if (agentResult.rows.length && agentResult.rows[0].twilio_phone_number === number.phone_number) {
    const nextNumber = await pool.query(
      "SELECT phone_number FROM customer_numbers WHERE customer_id = $1 AND status = 'active' AND id != $2 LIMIT 1",
      [number.customer_id, number.id]
    );
    const newPhone = nextNumber.rows[0]?.phone_number || null;
    await pool.query(
      'UPDATE agents SET twilio_phone_number = $1, updated_at = NOW() WHERE customer_id = $2',
      [newPhone, number.customer_id]
    );
    return newPhone;
  }
  return null;
}

// Suspend a phone number (temporary — can be reactivated)
router.patch('/numbers/:id/suspend', adminAuth, async (req, res) => {
  try {
    const { rows } = await pool.query(
      "UPDATE customer_numbers SET status = 'suspended' WHERE id = $1 RETURNING *",
      [req.params.id]
    );
    if (!rows.length) return res.status(404).json({ error: 'Not found' });
    await detachNumberFromAgent(rows[0]);
    logger.info({ numberId: req.params.id, phoneNumber: rows[0].phone_number }, 'Number suspended');
    res.json(rows[0]);
  } catch (err) {
    logger.error(err, 'Suspend number error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Reactivate a suspended number
router.patch('/numbers/:id/reactivate', adminAuth, async (req, res) => {
  try {
    const { rows } = await pool.query(
      "UPDATE customer_numbers SET status = 'active' WHERE id = $1 AND status = 'suspended' RETURNING *",
      [req.params.id]
    );
    if (!rows.length) return res.status(404).json({ error: 'Not found or not suspended' });

    // If customer has an agent but no active number, set this one
    const agentResult = await pool.query(
      'SELECT id, twilio_phone_number FROM agents WHERE customer_id = $1',
      [rows[0].customer_id]
    );
    if (agentResult.rows.length && !agentResult.rows[0].twilio_phone_number) {
      await pool.query(
        'UPDATE agents SET twilio_phone_number = $1, updated_at = NOW() WHERE customer_id = $2',
        [rows[0].phone_number, rows[0].customer_id]
      );
    }

    logger.info({ numberId: req.params.id, phoneNumber: rows[0].phone_number }, 'Number reactivated');
    res.json(rows[0]);
  } catch (err) {
    logger.error(err, 'Reactivate number error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Revoke a phone number (permanent)
router.patch('/numbers/:id/revoke', adminAuth, async (req, res) => {
  try {
    const { rows } = await pool.query(
      "UPDATE customer_numbers SET status = 'revoked' WHERE id = $1 RETURNING *",
      [req.params.id]
    );
    if (!rows.length) return res.status(404).json({ error: 'Not found' });
    await detachNumberFromAgent(rows[0]);
    logger.info({ numberId: req.params.id, phoneNumber: rows[0].phone_number }, 'Number revoked');
    res.json(rows[0]);
  } catch (err) {
    logger.error(err, 'Revoke number error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// ─── RECENT ASSIGNMENTS (for dashboard) ───
router.get('/recent-assignments', adminAuth, async (req, res) => {
  try {
    const minutes = await pool.query(
      `SELECT cm.*, c.email, c.name AS customer_name
       FROM customer_minutes cm JOIN customers c ON cm.customer_id = c.id
       ORDER BY cm.assigned_at DESC LIMIT 10`
    );
    const numbers = await pool.query(
      `SELECT cn.*, c.email, c.name AS customer_name,
              CASE WHEN a.id IS NOT NULL AND a.twilio_phone_number = cn.phone_number THEN true ELSE false END AS activated
       FROM customer_numbers cn
       JOIN customers c ON cn.customer_id = c.id
       LEFT JOIN agents a ON a.customer_id = cn.customer_id
       ORDER BY cn.assigned_at DESC LIMIT 20`
    );
    res.json({ recentMinutes: minutes.rows, recentNumbers: numbers.rows });
  } catch (err) {
    logger.error(err, 'Admin recent assignments error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// ─── ACTIVATE NUMBER (create agent linked to customer) ───

router.post('/customers/:id/activate-number', adminAuth, async (req, res) => {
  try {
    const customerId = req.params.id;
    const { phoneNumber } = req.body;
    if (!phoneNumber) {
      return res.status(400).json({ error: 'Phone number is required' });
    }

    // Get customer
    const customer = await customerService.findById(customerId);
    if (!customer) return res.status(404).json({ error: 'Customer not found' });

    // Check if customer already has an agent
    const existing = await pool.query('SELECT id FROM agents WHERE customer_id = $1', [customerId]);

    const twilioIdentity = `customer_${customerId}`;

    if (existing.rows.length > 0) {
      // Update existing agent with new number
      const { rows } = await pool.query(
        `UPDATE agents SET twilio_phone_number = $1, twilio_identity = $2, display_name = $3, status = 'available', updated_at = NOW()
         WHERE customer_id = $4 RETURNING *`,
        [phoneNumber, twilioIdentity, customer.name || customer.email, customerId]
      );
      logger.info({ customerId, phoneNumber, agentId: rows[0].id }, 'Updated agent number for customer');
      res.json({ agent: rows[0], message: 'Number updated for customer' });
    } else {
      // Create new agent linked to customer
      const hash = await bcrypt.hash(`cust_${customerId}_${Date.now()}`, 10);
      const { rows } = await pool.query(
        `INSERT INTO agents (username, password_hash, display_name, twilio_identity, twilio_phone_number, status, customer_id)
         VALUES ($1, $2, $3, $4, $5, 'available', $6) RETURNING *`,
        [`customer_${customerId}`, hash, customer.name || customer.email, twilioIdentity, phoneNumber, customerId]
      );
      logger.info({ customerId, phoneNumber, agentId: rows[0].id }, 'Created agent for customer');
      res.json({ agent: rows[0], message: 'Number activated for customer' });
    }
  } catch (err) {
    logger.error(err, 'Activate number error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Get customer's linked agent info
router.get('/customers/:id/agent', adminAuth, async (req, res) => {
  try {
    const { rows } = await pool.query(
      'SELECT id, username, display_name, twilio_identity, twilio_phone_number, status, customer_id FROM agents WHERE customer_id = $1',
      [req.params.id]
    );
    res.json({ agent: rows[0] || null });
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
