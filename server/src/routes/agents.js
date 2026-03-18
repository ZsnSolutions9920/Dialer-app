const express = require('express');
const agentService = require('../services/agentService');
const authMiddleware = require('../middleware/auth');

const router = express.Router();

router.get('/', authMiddleware, async (req, res) => {
  try {
    const agents = await agentService.listAll();
    res.json(agents);
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

router.get('/me', authMiddleware, async (req, res) => {
  try {
    const profile = await agentService.getProfile(req.agent.id);
    if (!profile) return res.status(404).json({ error: 'Agent not found' });
    res.json(profile);
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

router.patch('/me', authMiddleware, async (req, res) => {
  try {
    const updated = await agentService.updateProfile(req.agent.id, req.body);
    if (!updated) return res.status(404).json({ error: 'Agent not found' });
    res.json(updated);
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

router.patch('/:id/status', authMiddleware, async (req, res) => {
  try {
    const { id } = req.params;
    const { status } = req.body;

    const validStatuses = ['available', 'on_call', 'away', 'offline'];
    if (!validStatuses.includes(status)) {
      return res.status(400).json({ error: `Invalid status. Must be one of: ${validStatuses.join(', ')}` });
    }

    // Agents can only update their own status
    if (parseInt(id, 10) !== req.agent.id) {
      return res.status(403).json({ error: 'Cannot update another agent\'s status' });
    }

    const agent = await agentService.updateStatus(id, status);
    if (!agent) {
      return res.status(404).json({ error: 'Agent not found' });
    }

    // Broadcast status change via Socket.IO
    const io = req.app.get('io');
    if (io) {
      io.emit('agent:status', agent);
    }

    res.json(agent);
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
