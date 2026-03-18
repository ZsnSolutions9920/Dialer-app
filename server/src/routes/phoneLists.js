const express = require('express');
const phoneListService = require('../services/phoneListService');
const logger = require('../utils/logger');

const router = express.Router();

// Create a new phone list
router.post('/', async (req, res) => {
  try {
    const { name, totalCount } = req.body;
    if (!name || !totalCount) {
      return res.status(400).json({ error: 'name and totalCount are required' });
    }
    const list = await phoneListService.createList({
      name,
      agentId: req.agent.id,
      totalCount,
    });
    res.status(201).json(list);
  } catch (err) {
    logger.error(err, 'Error creating phone list');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Add entries to an existing list (batch upload)
router.post('/:id/entries', async (req, res) => {
  try {
    const { entries } = req.body;
    if (!Array.isArray(entries) || entries.length === 0) {
      return res.status(400).json({ error: 'a non-empty entries array is required' });
    }
    await phoneListService.addEntries(req.params.id, entries);
    res.status(201).json({ ok: true, count: entries.length });
  } catch (err) {
    logger.error(err, 'Error adding entries');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Get all lists for the authenticated agent
router.get('/', async (req, res) => {
  try {
    const lists = await phoneListService.getLists(req.agent.id);
    res.json(lists);
  } catch (err) {
    logger.error(err, 'Error fetching phone lists');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Get follow-ups for the authenticated agent within a date range
router.get('/follow-ups', async (req, res) => {
  try {
    const { start, end } = req.query;
    if (!start || !end) {
      return res.status(400).json({ error: 'start and end query params are required' });
    }
    const followUps = await phoneListService.getFollowUps(req.agent.id, start, end);
    res.json(followUps);
  } catch (err) {
    logger.error(err, 'Error fetching follow-ups');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Get next dialable entry for power dialer
router.get('/:id/next-dialable', async (req, res) => {
  try {
    const skipIds = req.query.skip
      ? req.query.skip.split(',').map((s) => parseInt(s, 10)).filter((n) => !isNaN(n))
      : [];
    const minId = req.query.minId ? parseInt(req.query.minId, 10) : null;
    const forceId = req.query.forceId ? parseInt(req.query.forceId, 10) : null;
    const entry = await phoneListService.getNextDialableEntry(
      req.params.id,
      skipIds,
      isNaN(minId) ? null : minId,
      forceId != null && !isNaN(forceId) ? forceId : null
    );
    res.json(entry);
  } catch (err) {
    logger.error(err, 'Error fetching next dialable entry');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Get power dial progress for a list
router.get('/:id/power-dial-progress', async (req, res) => {
  try {
    const progress = await phoneListService.getPowerDialProgress(req.params.id);
    res.json(progress);
  } catch (err) {
    logger.error(err, 'Error fetching power dial progress');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Get paginated entries for a list
router.get('/:id/entries', async (req, res) => {
  try {
    const page = parseInt(req.query.page, 10) || 1;
    const limit = parseInt(req.query.limit, 10) || 20;
    const search = req.query.search || '';
    const result = await phoneListService.getListEntries({
      listId: req.params.id,
      page,
      limit,
      search,
    });
    res.json(result);
  } catch (err) {
    logger.error(err, 'Error fetching list entries');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Get a single entry by ID
router.get('/entries/:entryId', async (req, res) => {
  try {
    const entry = await phoneListService.getEntry(req.params.entryId);
    if (!entry) return res.status(404).json({ error: 'Entry not found' });
    res.json(entry);
  } catch (err) {
    logger.error(err, 'Error fetching entry');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Update entry status
router.patch('/entries/:entryId/status', async (req, res) => {
  try {
    const { status, followUpAt, notes } = req.body;
    const valid = ['pending', 'called', 'no_answer', 'follow_up', 'not_interested', 'do_not_contact'];
    if (!valid.includes(status)) {
      return res.status(400).json({ error: `Invalid status. Must be one of: ${valid.join(', ')}` });
    }
    if (status === 'follow_up') {
      if (!followUpAt || isNaN(new Date(followUpAt).getTime())) {
        return res.status(400).json({ error: 'A valid followUpAt date is required for follow_up status' });
      }
    }
    const entry = await phoneListService.updateEntryStatus(req.params.entryId, status, followUpAt || null, notes || null);
    if (!entry) return res.status(404).json({ error: 'Entry not found' });
    res.json(entry);
  } catch (err) {
    logger.error(err, 'Error updating entry status');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Mark an entry as called
router.patch('/:entryId/called', async (req, res) => {
  try {
    const entry = await phoneListService.markEntryCalled(req.params.entryId);
    if (!entry) return res.status(404).json({ error: 'Entry not found' });
    res.json(entry);
  } catch (err) {
    logger.error(err, 'Error marking entry as called');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Delete a list
router.delete('/:id', async (req, res) => {
  try {
    const deleted = await phoneListService.deleteList(req.params.id);
    if (!deleted) return res.status(404).json({ error: 'List not found' });
    res.json({ ok: true });
  } catch (err) {
    logger.error(err, 'Error deleting phone list');
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
