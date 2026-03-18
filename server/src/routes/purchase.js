const express = require('express');
const jwt = require('jsonwebtoken');
const config = require('../config');
const pool = require('../db/pool');
const logger = require('../utils/logger');

const router = express.Router();

// Lazy-init Stripe (only if key is set)
let stripe = null;
function getStripe() {
  if (!stripe && process.env.STRIPE_SECRET_KEY) {
    stripe = require('stripe')(process.env.STRIPE_SECRET_KEY);
  }
  return stripe;
}

// Customer auth middleware
function customerAuth(req, res, next) {
  const header = req.headers.authorization;
  if (!header || !header.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Unauthorized' });
  }
  try {
    const payload = jwt.verify(header.slice(7), config.jwtSecret);
    if (payload.role !== 'customer') {
      return res.status(401).json({ error: 'Not a customer token' });
    }
    req.customer = payload;
    next();
  } catch {
    return res.status(401).json({ error: 'Invalid token' });
  }
}

// ─── MOCK PURCHASE (for testing without Stripe) ───

router.post('/mock', customerAuth, async (req, res) => {
  try {
    const { type, itemId, itemLabel, amount, metadata } = req.body;
    if (!type || !['minutes', 'number'].includes(type)) {
      return res.status(400).json({ error: 'Invalid purchase type' });
    }

    // Create purchase record
    const { rows } = await pool.query(
      `INSERT INTO purchases (customer_id, type, item_id, item_label, amount, status, mock, metadata, completed_at)
       VALUES ($1, $2, $3, $4, $5, 'completed', true, $6, NOW()) RETURNING *`,
      [req.customer.id, type, itemId || null, itemLabel || null, amount || 0, JSON.stringify(metadata || {})]
    );

    const purchase = rows[0];

    // Auto-assign based on type
    if (type === 'minutes' && metadata?.minutes) {
      await pool.query(
        `INSERT INTO customer_minutes (customer_id, minutes_total, package_id, price_paid, notes)
         VALUES ($1, $2, $3, $4, $5)`,
        [req.customer.id, metadata.minutes, itemId, amount, 'Mock purchase']
      );
    }

    if (type === 'number' && metadata?.phoneNumber) {
      await pool.query(
        `INSERT INTO customer_numbers (customer_id, phone_number, friendly_name, type, monthly_price, notes)
         VALUES ($1, $2, $3, $4, $5, $6)`,
        [req.customer.id, metadata.phoneNumber, metadata.friendlyName || null, metadata.numberType || 'local', metadata.monthlyPrice || 0, 'Mock purchase']
      );
    }

    logger.info({ customerId: req.customer.id, type, mock: true }, 'Mock purchase completed');
    res.status(201).json({ purchase, message: 'Mock purchase completed' });
  } catch (err) {
    logger.error(err, 'Mock purchase error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// ─── STRIPE CHECKOUT ───

router.post('/checkout', customerAuth, async (req, res) => {
  try {
    const s = getStripe();
    if (!s) {
      return res.status(503).json({ error: 'Stripe not configured. Use mock purchase for testing.' });
    }

    const { type, itemId, itemLabel, amount, metadata } = req.body;
    if (!type || !['minutes', 'number'].includes(type)) {
      return res.status(400).json({ error: 'Invalid purchase type' });
    }

    const baseUrl = config.serverBaseUrl.replace('/api', '');

    // Create purchase record
    const { rows } = await pool.query(
      `INSERT INTO purchases (customer_id, type, item_id, item_label, amount, metadata)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
      [req.customer.id, type, itemId || null, itemLabel || null, amount || 0, JSON.stringify(metadata || {})]
    );
    const purchase = rows[0];

    // Create Stripe checkout session
    const session = await s.checkout.sessions.create({
      payment_method_types: ['card'],
      line_items: [{
        price_data: {
          currency: 'usd',
          product_data: { name: itemLabel || `${type} purchase` },
          unit_amount: Math.round((amount || 0) * 100),
        },
        quantity: 1,
      }],
      mode: 'payment',
      success_url: `${baseUrl}/purchase-success?session_id={CHECKOUT_SESSION_ID}`,
      cancel_url: `${baseUrl}/purchase-cancel`,
      metadata: {
        purchaseId: purchase.id.toString(),
        customerId: req.customer.id.toString(),
        type,
        itemId: itemId || '',
      },
    });

    // Save session ID
    await pool.query(
      'UPDATE purchases SET stripe_session_id = $1 WHERE id = $2',
      [session.id, purchase.id]
    );

    res.json({ checkoutUrl: session.url, purchaseId: purchase.id });
  } catch (err) {
    logger.error(err, 'Stripe checkout error');
    res.status(500).json({ error: 'Failed to create checkout' });
  }
});

// ─── STRIPE WEBHOOK ───

router.post('/webhook', express.raw({ type: 'application/json' }), async (req, res) => {
  const s = getStripe();
  if (!s) return res.status(503).send('Stripe not configured');

  const sig = req.headers['stripe-signature'];
  const endpointSecret = process.env.STRIPE_WEBHOOK_SECRET;

  let event;
  try {
    event = s.webhooks.constructEvent(req.body, sig, endpointSecret);
  } catch (err) {
    logger.error(err, 'Stripe webhook signature failed');
    return res.status(400).send('Webhook signature verification failed');
  }

  if (event.type === 'checkout.session.completed') {
    const session = event.data.object;

    // Handle setup mode (card saving)
    if (session.mode === 'setup') {
      const customerId = session.metadata?.customerId;
      if (customerId && session.setup_intent) {
        try {
          const setupIntent = await s.setupIntents.retrieve(session.setup_intent);
          if (setupIntent.payment_method) {
            const pm = await s.paymentMethods.retrieve(setupIntent.payment_method);
            await pool.query(
              'UPDATE customers SET stripe_card_last4 = $1, stripe_card_brand = $2 WHERE id = $3',
              [pm.card?.last4 || null, pm.card?.brand || null, customerId]
            );
            logger.info({ customerId }, 'Card saved via setup session');
          }
        } catch (e) {
          logger.error(e, 'Failed to save card info from setup session');
        }
      }
    }

    // Handle payment mode (purchase)
    if (session.mode === 'payment') {
      const { purchaseId, customerId, type, itemId } = session.metadata || {};

      const { rows } = await pool.query(
        `UPDATE purchases SET status = 'completed', stripe_payment_id = $1, completed_at = NOW()
         WHERE id = $2 RETURNING *`,
        [session.payment_intent, purchaseId]
      );

      if (rows.length) {
        const purchase = rows[0];
        const meta = purchase.metadata;

        if (type === 'minutes' && meta.minutes) {
          await pool.query(
            `INSERT INTO customer_minutes (customer_id, minutes_total, package_id, price_paid, notes)
             VALUES ($1, $2, $3, $4, $5)`,
            [customerId, meta.minutes, itemId, purchase.amount, 'Stripe purchase']
          );
        }
        if (type === 'number' && meta.phoneNumber) {
          await pool.query(
            `INSERT INTO customer_numbers (customer_id, phone_number, friendly_name, type, monthly_price, notes)
             VALUES ($1, $2, $3, $4, $5, $6)`,
            [customerId, meta.phoneNumber, meta.friendlyName || null, meta.numberType || 'local', meta.monthlyPrice || 0, 'Stripe purchase']
          );
        }

        logger.info({ purchaseId, customerId, type }, 'Stripe checkout purchase completed');
      }
    }
  }

  res.json({ received: true });
});

// ─── PURCHASE HISTORY ───

router.get('/history', customerAuth, async (req, res) => {
  try {
    const { rows } = await pool.query(
      `SELECT * FROM purchases WHERE customer_id = $1 ORDER BY created_at DESC LIMIT 50`,
      [req.customer.id]
    );
    res.json({ purchases: rows });
  } catch (err) {
    logger.error(err, 'Purchase history error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// ─── CUSTOMER'S ASSIGNED RESOURCES ───

router.get('/my-minutes', customerAuth, async (req, res) => {
  try {
    const summary = await pool.query(
      `SELECT COALESCE(SUM(minutes_total), 0) AS total_minutes,
              COALESCE(SUM(minutes_used), 0) AS used_minutes,
              COALESCE(SUM(minutes_total) - SUM(minutes_used), 0) AS remaining_minutes
       FROM customer_minutes WHERE customer_id = $1`,
      [req.customer.id]
    );
    res.json(summary.rows[0]);
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

router.get('/my-numbers', customerAuth, async (req, res) => {
  try {
    const { rows } = await pool.query(
      "SELECT * FROM customer_numbers WHERE customer_id = $1 AND status = 'active' ORDER BY assigned_at DESC",
      [req.customer.id]
    );
    res.json({ numbers: rows });
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

// ─── CUSTOMER CALL HISTORY ───

router.get('/my-calls', customerAuth, async (req, res) => {
  try {
    const { page = 1, limit = 20 } = req.query;
    const offset = (parseInt(page) - 1) * parseInt(limit);

    // Get agent linked to this customer
    const agentResult = await pool.query('SELECT id FROM agents WHERE customer_id = $1', [req.customer.id]);
    if (!agentResult.rows.length) {
      return res.json({ calls: [], total: 0 });
    }
    const agentId = agentResult.rows[0].id;

    const countResult = await pool.query('SELECT COUNT(*) FROM call_logs WHERE agent_id = $1', [agentId]);
    const total = parseInt(countResult.rows[0].count, 10);

    const { rows } = await pool.query(
      `SELECT id, call_sid, direction, from_number, to_number, status,
              duration_seconds AS duration, notes, disposition, created_at
       FROM call_logs WHERE agent_id = $1
       ORDER BY created_at DESC LIMIT $2 OFFSET $3`,
      [agentId, parseInt(limit), offset]
    );

    res.json({ calls: rows, total, page: parseInt(page), limit: parseInt(limit) });
  } catch (err) {
    logger.error(err, 'Customer call history error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// ─── SELECT ACTIVE NUMBER ───

router.post('/select-number', customerAuth, async (req, res) => {
  try {
    const { phoneNumber } = req.body;
    if (!phoneNumber) return res.status(400).json({ error: 'Phone number required' });

    // Verify customer owns this number and it's active
    const { rows: numRows } = await pool.query(
      "SELECT * FROM customer_numbers WHERE customer_id = $1 AND phone_number = $2 AND status = 'active'",
      [req.customer.id, phoneNumber]
    );
    if (!numRows.length) return res.status(404).json({ error: 'Number not found or not active' });

    // Update agent record
    const { rows: agentRows } = await pool.query(
      'UPDATE agents SET twilio_phone_number = $1, updated_at = NOW() WHERE customer_id = $2 RETURNING *',
      [phoneNumber, req.customer.id]
    );

    if (!agentRows.length) {
      return res.status(400).json({ error: 'No agent account. Contact support to activate your number.' });
    }

    res.json({ selected: phoneNumber, agent: agentRows[0] });
  } catch (err) {
    logger.error(err, 'Select number error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// ─── TWILIO TOKEN FOR CUSTOMER ───

router.get('/twilio-token', customerAuth, async (req, res) => {
  try {
    // Find the agent linked to this customer
    const { rows } = await pool.query(
      'SELECT id, twilio_identity, twilio_phone_number FROM agents WHERE customer_id = $1',
      [req.customer.id]
    );

    if (!rows.length || !rows[0].twilio_identity) {
      return res.status(404).json({ error: 'No phone number activated. Purchase a number first.' });
    }

    const agent = rows[0];
    const { generateAccessToken } = require('../services/tokenService');
    const token = generateAccessToken(agent.twilio_identity);
    res.json({ token, identity: agent.twilio_identity, phoneNumber: agent.twilio_phone_number });
  } catch (err) {
    logger.error(err, 'Customer Twilio token error');
    res.status(500).json({ error: 'Failed to generate token' });
  }
});

// ─── STRIPE CARD MANAGEMENT ───

// Get or create Stripe customer for this user
async function getOrCreateStripeCustomer(customerId) {
  const s = getStripe();
  if (!s) return null;

  const { rows } = await pool.query('SELECT * FROM customers WHERE id = $1', [customerId]);
  if (!rows.length) return null;
  const customer = rows[0];

  if (customer.stripe_customer_id) return customer.stripe_customer_id;

  // Create new Stripe customer
  const sc = await s.customers.create({
    email: customer.email,
    name: customer.name || undefined,
    metadata: { customerId: customerId.toString() },
  });

  await pool.query('UPDATE customers SET stripe_customer_id = $1 WHERE id = $2', [sc.id, customerId]);
  return sc.id;
}

// Get saved payment info
router.get('/payment-method', customerAuth, async (req, res) => {
  try {
    const { rows } = await pool.query(
      'SELECT stripe_customer_id, stripe_card_last4, stripe_card_brand FROM customers WHERE id = $1',
      [req.customer.id]
    );
    const c = rows[0];
    if (c && c.stripe_card_last4) {
      res.json({ hasCard: true, last4: c.stripe_card_last4, brand: c.stripe_card_brand });
    } else {
      res.json({ hasCard: false, last4: null, brand: null });
    }
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Create Stripe Setup session to save a card
router.post('/setup-card', customerAuth, async (req, res) => {
  try {
    const s = getStripe();
    if (!s) return res.status(503).json({ error: 'Stripe not configured yet. Payment will be available soon.' });

    const stripeCustomerId = await getOrCreateStripeCustomer(req.customer.id);
    const baseUrl = config.serverBaseUrl.replace('/api', '');

    const session = await s.checkout.sessions.create({
      customer: stripeCustomerId,
      mode: 'setup',
      payment_method_types: ['card'],
      success_url: `${baseUrl}/card-saved?session_id={CHECKOUT_SESSION_ID}`,
      cancel_url: `${baseUrl}/card-cancel`,
      metadata: { customerId: req.customer.id.toString() },
    });

    res.json({ url: session.url });
  } catch (err) {
    logger.error(err, 'Setup card error');
    res.status(500).json({ error: 'Failed to create card setup session' });
  }
});

// Charge saved card for a purchase
router.post('/charge', customerAuth, async (req, res) => {
  try {
    const s = getStripe();
    if (!s) return res.status(503).json({ error: 'Stripe not configured' });

    const { type, itemId, itemLabel, amount, metadata } = req.body;
    if (!type || !['minutes', 'number'].includes(type)) {
      return res.status(400).json({ error: 'Invalid purchase type' });
    }

    // Get Stripe customer
    const { rows: custRows } = await pool.query('SELECT stripe_customer_id FROM customers WHERE id = $1', [req.customer.id]);
    const stripeCustomerId = custRows[0]?.stripe_customer_id;
    if (!stripeCustomerId) {
      return res.status(400).json({ error: 'No payment method saved. Please add a card first.' });
    }

    // Get default payment method
    const paymentMethods = await s.paymentMethods.list({ customer: stripeCustomerId, type: 'card', limit: 1 });
    if (!paymentMethods.data.length) {
      return res.status(400).json({ error: 'No card on file. Please add a card first.' });
    }

    const amountCents = Math.round((amount || 0) * 100);
    if (amountCents <= 0) {
      return res.status(400).json({ error: 'Invalid amount' });
    }

    // Create purchase record
    const { rows } = await pool.query(
      `INSERT INTO purchases (customer_id, type, item_id, item_label, amount, metadata)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
      [req.customer.id, type, itemId || null, itemLabel || null, amount || 0, JSON.stringify(metadata || {})]
    );
    const purchase = rows[0];

    // Charge the card
    const paymentIntent = await s.paymentIntents.create({
      amount: amountCents,
      currency: 'usd',
      customer: stripeCustomerId,
      payment_method: paymentMethods.data[0].id,
      off_session: true,
      confirm: true,
      metadata: {
        purchaseId: purchase.id.toString(),
        customerId: req.customer.id.toString(),
        type,
      },
    });

    // Mark as completed
    await pool.query(
      `UPDATE purchases SET status = 'completed', stripe_payment_id = $1, completed_at = NOW() WHERE id = $2`,
      [paymentIntent.id, purchase.id]
    );

    // Auto-assign
    if (type === 'minutes' && metadata?.minutes) {
      await pool.query(
        `INSERT INTO customer_minutes (customer_id, minutes_total, package_id, price_paid, notes)
         VALUES ($1, $2, $3, $4, $5)`,
        [req.customer.id, metadata.minutes, itemId, amount, 'Card purchase']
      );
    }
    if (type === 'number' && metadata?.phoneNumber) {
      await pool.query(
        `INSERT INTO customer_numbers (customer_id, phone_number, friendly_name, type, monthly_price, notes)
         VALUES ($1, $2, $3, $4, $5, $6)`,
        [req.customer.id, metadata.phoneNumber, metadata.friendlyName || null, metadata.numberType || 'local', metadata.monthlyPrice || 0, 'Card purchase']
      );
    }

    logger.info({ purchaseId: purchase.id, customerId: req.customer.id, type, amount }, 'Card purchase completed');
    res.json({ purchase: { ...purchase, status: 'completed' }, message: 'Purchase successful!' });
  } catch (err) {
    if (err.type === 'StripeCardError') {
      return res.status(402).json({ error: 'Card declined: ' + err.message });
    }
    logger.error(err, 'Charge error');
    res.status(500).json({ error: 'Payment failed' });
  }
});

// ─── CUSTOMER CALLING STATUS ───

router.get('/calling-status', customerAuth, async (req, res) => {
  try {
    // Check if customer has agent + number
    const agentResult = await pool.query(
      'SELECT id, twilio_identity, twilio_phone_number, status FROM agents WHERE customer_id = $1',
      [req.customer.id]
    );
    const agent = agentResult.rows[0] || null;

    // Check minutes balance
    const minutesResult = await pool.query(
      `SELECT COALESCE(SUM(minutes_total), 0) AS total,
              COALESCE(SUM(minutes_used), 0) AS used,
              COALESCE(SUM(minutes_total) - SUM(minutes_used), 0) AS remaining
       FROM customer_minutes WHERE customer_id = $1`,
      [req.customer.id]
    );
    const minutes = minutesResult.rows[0];

    res.json({
      hasNumber: !!(agent && agent.twilio_phone_number),
      phoneNumber: agent?.twilio_phone_number || null,
      twilioIdentity: agent?.twilio_identity || null,
      minutesTotal: parseInt(minutes.total) || 0,
      minutesUsed: parseInt(minutes.used) || 0,
      minutesRemaining: parseInt(minutes.remaining) || 0,
      canMakeCalls: !!(agent && agent.twilio_phone_number && parseInt(minutes.remaining) > 0),
    });
  } catch (err) {
    logger.error(err, 'Calling status error');
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
