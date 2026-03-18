const express = require('express');
const trackingService = require('../services/trackingService');
const logger = require('../utils/logger');

const router = express.Router();

// ─── Open Tracking Pixel (no auth — called by email clients) ────────
router.get('/track/:token/open', async (req, res) => {
  try {
    const ip = req.headers['x-forwarded-for'] || req.socket.remoteAddress;
    const ua = req.headers['user-agent'] || '';
    const result = await trackingService.recordOpen(req.params.token, ip, ua);

    // Emit real-time notification to the agent
    if (result) {
      const io = req.app.get('io');
      const room = `agent:${result.agentId}`;
      io.to(room).emit('email:tracking', {
        type: 'open',
        event: result.event,
      });
    }
  } catch (err) {
    logger.error(err, 'Tracking pixel error');
  }

  // Always return the pixel regardless of errors
  res.set({
    'Content-Type': 'image/gif',
    'Content-Length': trackingService.TRACKING_PIXEL.length,
    'Cache-Control': 'no-store, no-cache, must-revalidate, private',
    'Pragma': 'no-cache',
    'Expires': '0',
  });
  res.end(trackingService.TRACKING_PIXEL);
});

// ─── Click Tracking Redirect (no auth — called by email recipients) ─
router.get('/track/:token/click', async (req, res) => {
  const url = req.query.url;
  if (!url) return res.status(400).send('Missing URL');

  try {
    const ip = req.headers['x-forwarded-for'] || req.socket.remoteAddress;
    const ua = req.headers['user-agent'] || '';
    const result = await trackingService.recordClick(req.params.token, url, ip, ua);

    // Emit real-time notification to the agent
    if (result) {
      const io = req.app.get('io');
      const room = `agent:${result.agentId}`;
      io.to(room).emit('email:tracking', {
        type: 'click',
        event: result.event,
      });
    }
  } catch (err) {
    logger.error(err, 'Click tracking error');
  }

  // Always redirect to the original URL
  res.redirect(302, url);
});

module.exports = router;
