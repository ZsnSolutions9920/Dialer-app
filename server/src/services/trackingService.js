const crypto = require('crypto');
const pool = require('../db/pool');
const config = require('../config');

// 1x1 transparent GIF pixel
const TRACKING_PIXEL = Buffer.from(
  'R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7',
  'base64'
);

function generateToken() {
  return crypto.randomBytes(32).toString('hex');
}

function getTrackingBaseUrl() {
  return config.serverBaseUrl.replace(/\/$/, '');
}

// Generate a tracking token for an email and return it
async function createTrackingToken(emailId) {
  const token = generateToken();
  await pool.query('UPDATE emails SET tracking_token = $1 WHERE id = $2', [token, emailId]);
  return token;
}

// Inject tracking pixel and wrap links in the email HTML body
function injectTracking(html, trackingToken) {
  if (!html || !trackingToken) return html;
  const base = getTrackingBaseUrl();

  // Inject open-tracking pixel before </body> or at end
  const pixelUrl = `${base}/api/email/track/${trackingToken}/open`;
  const pixel = `<img src="${pixelUrl}" width="1" height="1" style="display:none;border:0;" alt="" />`;
  let result = html;
  if (result.includes('</body>')) {
    result = result.replace('</body>', `${pixel}</body>`);
  } else {
    result += pixel;
  }

  // Wrap all <a href="..."> links with click tracking redirect
  result = result.replace(
    /<a\s([^>]*?)href=["']([^"'#][^"']*)["']([^>]*?)>/gi,
    (match, before, url, after) => {
      // Skip tracking for mailto: and tel: links
      if (url.startsWith('mailto:') || url.startsWith('tel:')) return match;
      const encodedUrl = encodeURIComponent(url);
      const trackUrl = `${base}/api/email/track/${trackingToken}/click?url=${encodedUrl}`;
      return `<a ${before}href="${trackUrl}"${after}>`;
    }
  );

  return result;
}

// Record an open event (notify only on first open per email)
async function recordOpen(trackingToken, ip, userAgent) {
  const { rows } = await pool.query(
    'SELECT id, agent_id, to_address FROM emails WHERE tracking_token = $1',
    [trackingToken]
  );
  if (rows.length === 0) return null;

  const email = rows[0];

  // Check if this email was already opened before
  const { rows: existing } = await pool.query(
    "SELECT id FROM email_tracking_events WHERE email_id = $1 AND event_type = 'open' LIMIT 1",
    [email.id]
  );
  const isFirstOpen = existing.length === 0;

  // Update email counters
  await pool.query(
    `UPDATE emails SET
      open_count = open_count + 1,
      first_opened_at = COALESCE(first_opened_at, NOW()),
      last_opened_at = NOW()
     WHERE id = $1`,
    [email.id]
  );

  // Check for campaign association
  const { rows: campaignRows } = await pool.query(
    `SELECT ecl.campaign_id FROM email_campaign_logs ecl
     JOIN email_campaigns ec ON ec.id = ecl.campaign_id
     WHERE ec.agent_id = $1 AND ecl.recipient_email = $2
     ORDER BY ecl.id DESC LIMIT 1`,
    [email.agent_id, email.to_address]
  );

  const campaignId = campaignRows[0]?.campaign_id || null;

  // Insert tracking event
  const { rows: eventRows } = await pool.query(
    `INSERT INTO email_tracking_events (agent_id, email_id, campaign_id, recipient_email, event_type, ip_address, user_agent)
     VALUES ($1, $2, $3, $4, 'open', $5, $6) RETURNING *`,
    [email.agent_id, email.id, campaignId, email.to_address, ip || null, userAgent || null]
  );

  // Only return result (triggers socket notification) on first open
  if (!isFirstOpen) return null;

  return { event: eventRows[0], agentId: email.agent_id };
}

// Record a click event (notify only on first click per email)
async function recordClick(trackingToken, url, ip, userAgent) {
  const { rows } = await pool.query(
    'SELECT id, agent_id, to_address FROM emails WHERE tracking_token = $1',
    [trackingToken]
  );
  if (rows.length === 0) return null;

  const email = rows[0];

  // Check if this email already has any click event
  const { rows: existing } = await pool.query(
    "SELECT id FROM email_tracking_events WHERE email_id = $1 AND event_type = 'click' LIMIT 1",
    [email.id]
  );
  const isFirstClick = existing.length === 0;

  // Update email counters
  await pool.query(
    `UPDATE emails SET
      click_count = click_count + 1,
      first_clicked_at = COALESCE(first_clicked_at, NOW())
     WHERE id = $1`,
    [email.id]
  );

  // Check for campaign association
  const { rows: campaignRows } = await pool.query(
    `SELECT ecl.campaign_id FROM email_campaign_logs ecl
     JOIN email_campaigns ec ON ec.id = ecl.campaign_id
     WHERE ec.agent_id = $1 AND ecl.recipient_email = $2
     ORDER BY ecl.id DESC LIMIT 1`,
    [email.agent_id, email.to_address]
  );

  const campaignId = campaignRows[0]?.campaign_id || null;

  // Insert tracking event
  const { rows: eventRows } = await pool.query(
    `INSERT INTO email_tracking_events (agent_id, email_id, campaign_id, recipient_email, event_type, link_url, ip_address, user_agent)
     VALUES ($1, $2, $3, $4, 'click', $5, $6, $7) RETURNING *`,
    [email.agent_id, email.id, campaignId, email.to_address, url, ip || null, userAgent || null]
  );

  // Only return result (triggers socket notification) on first click
  if (!isFirstClick) return null;

  return { event: eventRows[0], agentId: email.agent_id };
}

// Get tracking events for an agent (notifications page)
async function getTrackingEvents(agentId, { page = 1, limit = 50 } = {}) {
  const offset = (page - 1) * limit;
  const { rows } = await pool.query(
    `SELECT te.*, e.subject AS email_subject, e.to_address
     FROM email_tracking_events te
     LEFT JOIN emails e ON e.id = te.email_id
     WHERE te.agent_id = $1
     ORDER BY te.created_at DESC
     LIMIT $2 OFFSET $3`,
    [agentId, limit, offset]
  );
  const { rows: countRows } = await pool.query(
    'SELECT COUNT(*)::int AS count FROM email_tracking_events WHERE agent_id = $1',
    [agentId]
  );
  return { events: rows, total: countRows[0].count, page, limit };
}

// Get tracking stats for a specific email
async function getEmailTrackingStats(emailId, agentId) {
  const { rows: email } = await pool.query(
    'SELECT open_count, click_count, first_opened_at, last_opened_at, first_clicked_at FROM emails WHERE id = $1 AND agent_id = $2',
    [emailId, agentId]
  );
  if (email.length === 0) return null;

  const { rows: events } = await pool.query(
    `SELECT event_type, link_url, ip_address, created_at
     FROM email_tracking_events
     WHERE email_id = $1 AND agent_id = $2
     ORDER BY created_at DESC
     LIMIT 50`,
    [emailId, agentId]
  );

  return { ...email[0], events };
}

module.exports = {
  TRACKING_PIXEL,
  generateToken,
  createTrackingToken,
  injectTracking,
  recordOpen,
  recordClick,
  getTrackingEvents,
  getEmailTrackingStats,
};
