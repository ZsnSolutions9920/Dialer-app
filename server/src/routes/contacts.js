const express = require('express');
const contactService = require('../services/contactService');
const logger = require('../utils/logger');

const router = express.Router();

// List contacts with search and pagination
router.get('/', async (req, res) => {
  try {
    const page = parseInt(req.query.page, 10) || 1;
    const limit = parseInt(req.query.limit, 10) || 50;
    const { search } = req.query;
    const result = await contactService.getContacts({ agentId: null, search, page, limit });
    res.json(result);
  } catch (err) {
    logger.error(err, 'Error fetching contacts');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Lookup contact by phone number (before /:id to avoid conflict)
router.get('/lookup/:phoneNumber', async (req, res) => {
  try {
    const contact = await contactService.getContactByPhone(req.params.phoneNumber);
    if (!contact) return res.status(404).json({ error: 'Contact not found' });
    res.json(contact);
  } catch (err) {
    logger.error(err, 'Error looking up contact');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Get single contact
router.get('/:id', async (req, res) => {
  try {
    const contact = await contactService.getContactById(req.params.id);
    if (!contact) return res.status(404).json({ error: 'Contact not found' });
    res.json(contact);
  } catch (err) {
    logger.error(err, 'Error fetching contact');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Create contact
router.post('/', async (req, res) => {
  try {
    const { name, phone_number, email, company, notes } = req.body;
    if (!name || !phone_number) {
      return res.status(400).json({ error: 'name and phone_number are required' });
    }
    const contact = await contactService.createContact({
      name,
      phoneNumber: phone_number,
      email,
      company,
      notes,
      agentId: req.agent.id,
    });
    res.status(201).json(contact);
  } catch (err) {
    logger.error(err, 'Error creating contact');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Update contact
router.put('/:id', async (req, res) => {
  try {
    const { name, phone_number, email, company, notes } = req.body;
    const contact = await contactService.updateContact(req.params.id, {
      name,
      phoneNumber: phone_number,
      email,
      company,
      notes,
    });
    if (!contact) return res.status(404).json({ error: 'Contact not found' });
    res.json(contact);
  } catch (err) {
    logger.error(err, 'Error updating contact');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Toggle favorite
router.patch('/:id/favorite', async (req, res) => {
  try {
    const contact = await contactService.toggleFavorite(req.params.id);
    if (!contact) return res.status(404).json({ error: 'Contact not found' });
    res.json(contact);
  } catch (err) {
    logger.error(err, 'Error toggling favorite');
    res.status(500).json({ error: 'Internal server error' });
  }
});

// Delete contact
router.delete('/:id', async (req, res) => {
  try {
    const deleted = await contactService.deleteContact(req.params.id);
    if (!deleted) return res.status(404).json({ error: 'Contact not found' });
    res.json({ ok: true });
  } catch (err) {
    logger.error(err, 'Error deleting contact');
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
