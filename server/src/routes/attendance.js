const express = require('express');
const attendanceService = require('../services/attendanceService');
const authMiddleware = require('../middleware/auth');
const { sendAttendanceNotification } = require('../services/discordService');

const router = express.Router();

// Hardcoded admin credentials
const ADMIN_USERNAME = 'admin';
const ADMIN_PASSWORD = 'admin123';

router.post('/admin-verify', (req, res) => {
  const { username, password } = req.body;
  if (username === ADMIN_USERNAME && password === ADMIN_PASSWORD) {
    return res.json({ ok: true });
  }
  return res.status(401).json({ error: 'Invalid admin credentials' });
});

router.post('/clock-in', authMiddleware, async (req, res) => {
  try {
    const session = await attendanceService.clockIn(req.agent.id);
    res.json(session);

    sendAttendanceNotification({
      agentName: req.agent.displayName || req.agent.username,
      type: 'clock_in',
      timestamp: session.clock_in,
    });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.post('/clock-out', authMiddleware, async (req, res) => {
  try {
    const session = await attendanceService.clockOut(req.agent.id);
    res.json(session);

    sendAttendanceNotification({
      agentName: req.agent.displayName || req.agent.username,
      type: 'clock_out',
      timestamp: session.clock_out,
      durationSeconds: session.duration_seconds,
    });
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

router.get('/current', authMiddleware, async (req, res) => {
  try {
    const session = await attendanceService.getCurrentSession(req.agent.id);
    res.json(session);
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

router.get('/history/:agentId', authMiddleware, async (req, res) => {
  try {
    const { agentId } = req.params;
    const limit = parseInt(req.query.limit, 10) || 50;
    const offset = parseInt(req.query.offset, 10) || 0;
    const result = await attendanceService.getHistory(agentId, { limit, offset });
    res.json(result);
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
