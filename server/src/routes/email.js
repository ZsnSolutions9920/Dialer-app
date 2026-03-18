const express = require('express');
const multer = require('multer');
const path = require('path');
const emailService = require('../services/emailService');
const trackingService = require('../services/trackingService');
const googleOAuth = require('../services/googleOAuth');
const logger = require('../utils/logger');

const router = express.Router();

// Multer for attachments — store in memory for forwarding to email
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB per file
});

// ─── SMTP Config ────────────────────────────────────────────────────

router.get('/smtp', async (req, res) => {
  try {
    const configs = await emailService.getSmtpConfigs(req.agent.id);
    res.json(configs);
  } catch (err) {
    logger.error(err, 'Failed to get SMTP configs');
    res.status(500).json({ error: 'Failed to get SMTP configs' });
  }
});

router.post('/smtp', async (req, res) => {
  try {
    const config = await emailService.saveSmtpConfig(req.agent.id, req.body);
    res.status(201).json(config);
  } catch (err) {
    logger.error(err, 'Failed to save SMTP config');
    res.status(500).json({ error: 'Failed to save SMTP config' });
  }
});

router.put('/smtp/:id', async (req, res) => {
  try {
    const config = await emailService.updateSmtpConfig(parseInt(req.params.id), req.agent.id, req.body);
    if (!config) return res.status(404).json({ error: 'Not found' });
    res.json(config);
  } catch (err) {
    logger.error(err, 'Failed to update SMTP config');
    res.status(500).json({ error: 'Failed to update SMTP config' });
  }
});

router.delete('/smtp/:id', async (req, res) => {
  try {
    const ok = await emailService.deleteSmtpConfig(parseInt(req.params.id), req.agent.id);
    if (!ok) return res.status(404).json({ error: 'Not found' });
    res.json({ success: true });
  } catch (err) {
    logger.error(err, 'Failed to delete SMTP config');
    res.status(500).json({ error: 'Failed to delete SMTP config' });
  }
});

router.post('/smtp/:id/test', async (req, res) => {
  try {
    await emailService.testSmtpConnection(parseInt(req.params.id), req.agent.id);
    res.json({ success: true, message: 'SMTP connection successful' });
  } catch (err) {
    logger.error(err, 'SMTP test failed');
    res.status(400).json({ error: err.message || 'SMTP connection failed' });
  }
});

// ─── Google OAuth ────────────────────────────────────────────────────

router.get('/oauth/google/url', async (req, res) => {
  try {
    const url = googleOAuth.getAuthUrl(req.agent.id);
    res.json({ url });
  } catch (err) {
    logger.error(err, 'Failed to generate Google OAuth URL');
    res.status(400).json({ error: err.message || 'Google OAuth not configured' });
  }
});

router.post('/oauth/google/callback', async (req, res) => {
  try {
    const { code } = req.body;
    if (!code) return res.status(400).json({ error: 'Authorization code is required' });
    const result = await googleOAuth.handleCallback(code, req.agent.id);
    res.json({ success: true, ...result });
  } catch (err) {
    logger.error(err, 'Google OAuth callback failed');
    res.status(400).json({ error: err.message || 'Failed to connect Google account' });
  }
});

router.get('/oauth/google/status', async (req, res) => {
  try {
    res.json({
      configured: !!(process.env.GOOGLE_CLIENT_ID && process.env.GOOGLE_CLIENT_SECRET && process.env.GOOGLE_REDIRECT_URI),
    });
  } catch {
    res.json({ configured: false });
  }
});

// ─── Email Templates ────────────────────────────────────────────────

router.get('/templates', async (req, res) => {
  try {
    const templates = await emailService.getTemplates(req.agent.id);
    res.json(templates);
  } catch (err) {
    logger.error(err, 'Failed to get templates');
    res.status(500).json({ error: 'Failed to get templates' });
  }
});

router.post('/templates', async (req, res) => {
  try {
    const template = await emailService.createTemplate(req.agent.id, req.body);
    res.status(201).json(template);
  } catch (err) {
    logger.error(err, 'Failed to create template');
    res.status(500).json({ error: 'Failed to create template' });
  }
});

router.put('/templates/:id', async (req, res) => {
  try {
    const template = await emailService.updateTemplate(parseInt(req.params.id), req.agent.id, req.body);
    if (!template) return res.status(404).json({ error: 'Not found' });
    res.json(template);
  } catch (err) {
    logger.error(err, 'Failed to update template');
    res.status(500).json({ error: 'Failed to update template' });
  }
});

router.delete('/templates/:id', async (req, res) => {
  try {
    const ok = await emailService.deleteTemplate(parseInt(req.params.id), req.agent.id);
    if (!ok) return res.status(404).json({ error: 'Not found' });
    res.json({ success: true });
  } catch (err) {
    logger.error(err, 'Failed to delete template');
    res.status(500).json({ error: 'Failed to delete template' });
  }
});

// ─── Recipients Preview ─────────────────────────────────────────────

router.post('/recipients', async (req, res) => {
  try {
    const { listId, statusFilter } = req.body;
    const count = await emailService.countRecipients({ listId, statusFilter });
    res.json({ count });
  } catch (err) {
    logger.error(err, 'Failed to count recipients');
    res.status(500).json({ error: 'Failed to count recipients' });
  }
});

// ─── AI Email Generation ────────────────────────────────────────────

router.post('/generate-ai', async (req, res) => {
  try {
    const { context, tone, recipientName, recipientData } = req.body;
    const apiKey = process.env.OPENROUTER_API_KEY;
    if (!apiKey) return res.status(400).json({ error: 'OpenRouter API key not configured. Set OPENROUTER_API_KEY in your .env file.' });

    const systemPrompt = `You are an expert email writer. Generate a professional email based on the user's instructions.
Return ONLY a JSON object with "subject" and "body" fields. The body should be in HTML format suitable for email.
Tone: ${tone || 'professional'}
${recipientName ? `Recipient name: ${recipientName}` : ''}
${recipientData ? `Recipient data: ${JSON.stringify(recipientData)}` : ''}
Use template variables like {{name}}, {{email}}, {{trademark}}, {{serial number}} etc where appropriate so the email can be personalized per recipient.`;

    const response = await fetch('https://openrouter.ai/api/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${apiKey}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model: 'arcee-ai/trinity-large-preview:free',
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: context },
        ],
        temperature: 0.7,
      }),
    });

    if (!response.ok) {
      const errData = await response.text();
      throw new Error(`OpenRouter API error: ${errData}`);
    }

    const data = await response.json();
    const content = data.choices?.[0]?.message?.content || '';

    // Try to parse JSON from the response
    let parsed;
    try {
      // Handle markdown code blocks
      const jsonMatch = content.match(/```(?:json)?\s*([\s\S]*?)```/) || [null, content];
      parsed = JSON.parse(jsonMatch[1].trim());
    } catch {
      parsed = { subject: '', body: content };
    }

    res.json({ subject: parsed.subject || '', body: parsed.body || '' });
  } catch (err) {
    logger.error(err, 'AI generation failed');
    res.status(500).json({ error: err.message || 'AI generation failed' });
  }
});

// ─── Send Test Email ────────────────────────────────────────────────

router.post('/send-test', async (req, res) => {
  try {
    const { smtpConfigId, to, subject, body } = req.body;
    const smtpConfig = await emailService.getSmtpConfig(smtpConfigId, req.agent.id);
    if (!smtpConfig) return res.status(404).json({ error: 'SMTP config not found' });
    await emailService.sendEmail(smtpConfig, { to, subject, html: body });
    res.json({ success: true, message: 'Test email sent' });
  } catch (err) {
    logger.error(err, 'Test email failed');
    res.status(500).json({ error: err.message || 'Failed to send test email' });
  }
});

// ─── Send Single Email ──────────────────────────────────────────────

router.post('/send-single', upload.array('attachments', 5), async (req, res) => {
  try {
    const { smtpConfigId, to, cc, subject, body } = req.body;
    const smtpConfig = await emailService.getSmtpConfig(parseInt(smtpConfigId), req.agent.id);
    if (!smtpConfig) return res.status(404).json({ error: 'SMTP config not found' });

    const attachments = (req.files || []).map((f) => ({
      filename: f.originalname,
      content: f.buffer,
    }));

    const saved = await emailService.saveSentEmail(req.agent.id, { to, cc: cc || undefined, subject, html: body, smtpConfigId: parseInt(smtpConfigId) });
    const info = await emailService.sendEmail(smtpConfig, { to, cc: cc || undefined, subject, html: body, attachments, trackingToken: saved.tracking_token });
    res.json({ success: true, message: 'Email sent successfully' });
  } catch (err) {
    logger.error(err, 'Send single email failed');
    res.status(500).json({ error: err.message || 'Failed to send email' });
  }
});

// ─── Campaigns ──────────────────────────────────────────────────────

router.get('/campaigns', async (req, res) => {
  try {
    const campaigns = await emailService.getCampaigns(req.agent.id);
    res.json(campaigns);
  } catch (err) {
    logger.error(err, 'Failed to get campaigns');
    res.status(500).json({ error: 'Failed to get campaigns' });
  }
});

router.post('/campaigns', async (req, res) => {
  try {
    const { recipients, ...campaignData } = req.body;
    const campaign = await emailService.createCampaign(req.agent.id, campaignData);

    // If uploaded sheet recipients are provided, store them
    if (recipients && recipients.length > 0) {
      await emailService.addCampaignRecipients(campaign.id, recipients);
      // Update total count
      await emailService.updateCampaignStatus(campaign.id, 'draft', { total_recipients: recipients.length });
      campaign.total_recipients = recipients.length;
    }

    res.status(201).json(campaign);
  } catch (err) {
    logger.error(err, 'Failed to create campaign');
    res.status(500).json({ error: 'Failed to create campaign' });
  }
});

router.post('/campaigns/:id/start', async (req, res) => {
  try {
    const campaignId = parseInt(req.params.id);
    const campaign = await emailService.getCampaign(campaignId, req.agent.id);
    if (!campaign) return res.status(404).json({ error: 'Campaign not found' });
    if (campaign.status === 'sending') return res.status(400).json({ error: 'Campaign already sending' });

    res.json({ success: true, message: 'Campaign started' });

    // Run campaign in background
    const io = req.app.get('io');
    emailService.runCampaign(campaignId, req.agent.id, io).catch((err) => {
      logger.error(err, 'Campaign run failed');
      emailService.updateCampaignStatus(campaignId, 'failed');
    });
  } catch (err) {
    logger.error(err, 'Failed to start campaign');
    res.status(500).json({ error: 'Failed to start campaign' });
  }
});

router.get('/campaigns/:id', async (req, res) => {
  try {
    const campaign = await emailService.getCampaign(parseInt(req.params.id), req.agent.id);
    if (!campaign) return res.status(404).json({ error: 'Not found' });
    res.json(campaign);
  } catch (err) {
    logger.error(err, 'Failed to get campaign');
    res.status(500).json({ error: 'Failed to get campaign' });
  }
});

router.get('/campaigns/:id/logs', async (req, res) => {
  try {
    const logs = await emailService.getCampaignLogs(parseInt(req.params.id), req.query.status || null);
    res.json(logs);
  } catch (err) {
    logger.error(err, 'Failed to get campaign logs');
    res.status(500).json({ error: 'Failed to get campaign logs' });
  }
});

// ─── Inbox ──────────────────────────────────────────────────────────

router.post('/inbox/sync', async (req, res) => {
  try {
    const { smtpConfigId } = req.body;
    const result = await emailService.fetchInboxEmails(req.agent.id, smtpConfigId ? parseInt(smtpConfigId) : undefined);
    res.json({ success: true, ...result });
  } catch (err) {
    logger.error(err, 'Inbox sync failed');
    res.status(500).json({ error: err.message || 'Failed to sync inbox' });
  }
});

router.get('/inbox', async (req, res) => {
  try {
    const { folder, page, limit, search, smtpConfigId } = req.query;
    const result = await emailService.getEmails(req.agent.id, {
      folder: folder || 'all',
      page: parseInt(page) || 1,
      limit: parseInt(limit) || 30,
      search: search || '',
      smtpConfigId: smtpConfigId ? parseInt(smtpConfigId) : undefined,
    });
    res.json(result);
  } catch (err) {
    logger.error(err, 'Failed to get emails');
    res.status(500).json({ error: 'Failed to get emails' });
  }
});

router.get('/inbox/unread-count', async (req, res) => {
  try {
    const count = await emailService.getUnreadCount(req.agent.id);
    res.json({ count });
  } catch (err) {
    logger.error(err, 'Failed to get unread count');
    res.status(500).json({ error: 'Failed to get unread count' });
  }
});

router.get('/inbox/:id', async (req, res) => {
  try {
    const email = await emailService.getEmailById(parseInt(req.params.id), req.agent.id);
    if (!email) return res.status(404).json({ error: 'Email not found' });
    if (!email.is_read) {
      await emailService.markEmailRead(email.id, req.agent.id);
      email.is_read = true;
    }
    res.json(email);
  } catch (err) {
    logger.error(err, 'Failed to get email');
    res.status(500).json({ error: 'Failed to get email' });
  }
});

router.get('/inbox/:id/thread', async (req, res) => {
  try {
    const thread = await emailService.getEmailThread(parseInt(req.params.id), req.agent.id);
    res.json(thread);
  } catch (err) {
    logger.error(err, 'Failed to get email thread');
    res.status(500).json({ error: 'Failed to get email thread' });
  }
});

router.get('/list-columns/:listId', async (req, res) => {
  try {
    const columns = await emailService.getListColumns(parseInt(req.params.listId));
    res.json({ columns });
  } catch (err) {
    logger.error(err, 'Failed to get list columns');
    res.status(500).json({ error: 'Failed to get list columns' });
  }
});

router.delete('/inbox/:id', async (req, res) => {
  try {
    const ok = await emailService.deleteEmail(parseInt(req.params.id), req.agent.id);
    if (!ok) return res.status(404).json({ error: 'Email not found' });
    res.json({ success: true });
  } catch (err) {
    logger.error(err, 'Failed to delete email');
    res.status(500).json({ error: 'Failed to delete email' });
  }
});

router.post('/inbox/:id/reply', async (req, res) => {
  try {
    const { body, cc } = req.body;
    const result = await emailService.replyToEmail(req.agent.id, { emailId: parseInt(req.params.id), body, cc });
    res.json(result);
  } catch (err) {
    logger.error(err, 'Reply failed');
    res.status(500).json({ error: err.message || 'Failed to send reply' });
  }
});

router.post('/inbox/:id/forward', async (req, res) => {
  try {
    const { to, cc, body } = req.body;
    if (!to) return res.status(400).json({ error: 'Recipient (to) is required' });
    const result = await emailService.forwardEmail(req.agent.id, { emailId: parseInt(req.params.id), to, cc, body });
    res.json(result);
  } catch (err) {
    logger.error(err, 'Forward failed');
    res.status(500).json({ error: err.message || 'Failed to forward email' });
  }
});

// ─── Tracking / Notifications ────────────────────────────────────────

router.get('/tracking/events', async (req, res) => {
  try {
    const { page, limit } = req.query;
    const result = await trackingService.getTrackingEvents(req.agent.id, {
      page: parseInt(page) || 1,
      limit: parseInt(limit) || 50,
    });
    res.json(result);
  } catch (err) {
    logger.error(err, 'Failed to get tracking events');
    res.status(500).json({ error: 'Failed to get tracking events' });
  }
});

router.get('/tracking/email/:id', async (req, res) => {
  try {
    const stats = await trackingService.getEmailTrackingStats(parseInt(req.params.id), req.agent.id);
    if (!stats) return res.status(404).json({ error: 'Not found' });
    res.json(stats);
  } catch (err) {
    logger.error(err, 'Failed to get email tracking stats');
    res.status(500).json({ error: 'Failed to get tracking stats' });
  }
});

module.exports = router;
