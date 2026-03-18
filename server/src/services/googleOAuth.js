const { google } = require('googleapis');
const pool = require('../db/pool');

function getOAuth2Client() {
  const clientId = process.env.GOOGLE_CLIENT_ID;
  const clientSecret = process.env.GOOGLE_CLIENT_SECRET;
  const redirectUri = process.env.GOOGLE_REDIRECT_URI;
  if (!clientId || !clientSecret || !redirectUri) {
    throw new Error('Google OAuth not configured. Set GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, and GOOGLE_REDIRECT_URI in .env');
  }
  return new google.auth.OAuth2(clientId, clientSecret, redirectUri);
}

// Generate the Google consent URL
function getAuthUrl(agentId) {
  const oauth2Client = getOAuth2Client();
  return oauth2Client.generateAuthUrl({
    access_type: 'offline',
    prompt: 'consent',
    scope: [
      'https://mail.google.com/',
      'https://www.googleapis.com/auth/userinfo.email',
      'https://www.googleapis.com/auth/userinfo.profile',
    ],
    state: String(agentId),
  });
}

// Exchange auth code for tokens + fetch user profile, then save as SMTP config
async function handleCallback(code, agentId) {
  const oauth2Client = getOAuth2Client();
  const { tokens } = await oauth2Client.getToken(code);
  oauth2Client.setCredentials(tokens);

  // Fetch user's email and name from Google
  const oauth2 = google.oauth2({ version: 'v2', auth: oauth2Client });
  const { data: profile } = await oauth2.userinfo.get();
  const email = profile.email;
  const name = profile.name || '';

  // Check if an OAuth config already exists for this email + agent
  const { rows: existing } = await pool.query(
    "SELECT id FROM smtp_configs WHERE agent_id = $1 AND username = $2 AND auth_type = 'oauth'",
    [agentId, email]
  );

  if (existing.length > 0) {
    // Update tokens on existing config
    await pool.query(
      `UPDATE smtp_configs SET
        oauth_access_token = $1,
        oauth_refresh_token = COALESCE($2, oauth_refresh_token),
        oauth_token_expiry = $3,
        updated_at = NOW()
       WHERE id = $4`,
      [
        tokens.access_token,
        tokens.refresh_token || null,
        tokens.expiry_date ? new Date(tokens.expiry_date) : null,
        existing[0].id,
      ]
    );
    return { id: existing[0].id, updated: true, email };
  }

  // Create new SMTP config for Google OAuth
  const { rows } = await pool.query(
    `INSERT INTO smtp_configs
      (agent_id, label, host, port, secure, username, password, from_email, from_name, is_default,
       imap_host, imap_port, imap_secure, auth_type, oauth_provider, oauth_access_token, oauth_refresh_token, oauth_token_expiry)
     VALUES ($1, $2, 'smtp.gmail.com', 465, true, $3, NULL, $3, $4, false,
       'imap.gmail.com', 993, true, 'oauth', 'google', $5, $6, $7)
     RETURNING id`,
    [
      agentId,
      `Gmail - ${email}`,
      email,
      name,
      tokens.access_token,
      tokens.refresh_token || null,
      tokens.expiry_date ? new Date(tokens.expiry_date) : null,
    ]
  );

  return { id: rows[0].id, updated: false, email };
}

// Refresh the access token if expired and return a valid token
async function getValidAccessToken(smtpConfig) {
  // If token hasn't expired yet, return it
  if (smtpConfig.oauth_token_expiry && new Date(smtpConfig.oauth_token_expiry) > new Date(Date.now() + 60000)) {
    return smtpConfig.oauth_access_token;
  }

  // Token expired or about to expire — refresh it
  if (!smtpConfig.oauth_refresh_token) {
    throw new Error('OAuth refresh token missing. Please reconnect your Google account.');
  }

  const oauth2Client = getOAuth2Client();
  oauth2Client.setCredentials({ refresh_token: smtpConfig.oauth_refresh_token });
  const { credentials } = await oauth2Client.refreshAccessToken();

  // Save updated tokens
  await pool.query(
    `UPDATE smtp_configs SET
      oauth_access_token = $1,
      oauth_refresh_token = COALESCE($2, oauth_refresh_token),
      oauth_token_expiry = $3,
      updated_at = NOW()
     WHERE id = $4`,
    [
      credentials.access_token,
      credentials.refresh_token || null,
      credentials.expiry_date ? new Date(credentials.expiry_date) : null,
      smtpConfig.id,
    ]
  );

  return credentials.access_token;
}

module.exports = {
  getOAuth2Client,
  getAuthUrl,
  handleCallback,
  getValidAccessToken,
};
