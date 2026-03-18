const pool = require('../db/pool');
const nodemailer = require('nodemailer');
const { ImapFlow } = require('imapflow');
const { simpleParser } = require('mailparser');
const trackingService = require('./trackingService');
const googleOAuth = require('./googleOAuth');

// ─── Auth helpers ────────────────────────────────────────────────────

async function buildSmtpAuth(config) {
  if (config.auth_type === 'oauth' && config.oauth_provider === 'google') {
    const accessToken = await googleOAuth.getValidAccessToken(config);
    return { type: 'OAuth2', user: config.username, accessToken };
  }
  return { user: config.username, pass: config.password };
}

async function buildImapAuth(config) {
  if (config.auth_type === 'oauth' && config.oauth_provider === 'google') {
    const accessToken = await googleOAuth.getValidAccessToken(config);
    return { user: config.username, accessToken };
  }
  return { user: config.username, pass: config.password };
}

// ─── SMTP Config CRUD ───────────────────────────────────────────────

async function getSmtpConfigs(agentId) {
  const { rows } = await pool.query(
    'SELECT id, agent_id, label, host, port, secure, username, from_email, from_name, is_default, imap_host, imap_port, imap_secure, auth_type, oauth_provider, created_at FROM smtp_configs WHERE agent_id = $1 ORDER BY is_default DESC, created_at DESC',
    [agentId]
  );
  return rows;
}

async function getSmtpConfig(id, agentId) {
  const { rows } = await pool.query(
    'SELECT * FROM smtp_configs WHERE id = $1 AND agent_id = $2',
    [id, agentId]
  );
  return rows[0] || null;
}

async function saveSmtpConfig(agentId, config) {
  const { label, host, port, secure, username, password, from_email, from_name, imap_host, imap_port, imap_secure } = config;
  const { rows } = await pool.query(
    `INSERT INTO smtp_configs (agent_id, label, host, port, secure, username, password, from_email, from_name, is_default, imap_host, imap_port, imap_secure)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, true, $10, $11, $12)
     RETURNING id, agent_id, label, host, port, secure, username, from_email, from_name, is_default, imap_host, imap_port, imap_secure, created_at`,
    [agentId, label || 'Default', host, port || 587, secure || false, username, password, from_email, from_name || '', imap_host || null, imap_port || 993, imap_secure !== false]
  );
  return rows[0];
}

async function updateSmtpConfig(id, agentId, config) {
  const { label, host, port, secure, username, password, from_email, from_name, is_default, imap_host, imap_port, imap_secure } = config;
  if (is_default) {
    await pool.query('UPDATE smtp_configs SET is_default = false WHERE agent_id = $1 AND id != $2', [agentId, id]);
  }
  const setParts = ['label=$2', 'host=$3', 'port=$4', 'secure=$5', 'username=$6', 'from_email=$8', 'from_name=$9', 'is_default=$10', 'imap_host=$12', 'imap_port=$13', 'imap_secure=$14', 'updated_at=NOW()'];
  const params = [id, label, host, port, secure, username, password, from_email, from_name, is_default, agentId, imap_host || null, imap_port || 993, imap_secure !== false];
  // Only update password if provided
  if (password) {
    setParts.push('password=$7');
  }
  const { rows } = await pool.query(
    `UPDATE smtp_configs SET ${setParts.join(', ')} WHERE id = $1 AND agent_id = $11
     RETURNING id, agent_id, label, host, port, secure, username, from_email, from_name, is_default, imap_host, imap_port, imap_secure, created_at`,
    params
  );
  return rows[0] || null;
}

async function deleteSmtpConfig(id, agentId) {
  const { rowCount } = await pool.query('DELETE FROM smtp_configs WHERE id = $1 AND agent_id = $2', [id, agentId]);
  return rowCount > 0;
}

async function testSmtpConnection(id, agentId) {
  const config = await getSmtpConfig(id, agentId);
  if (!config) throw new Error('SMTP config not found');
  const auth = await buildSmtpAuth(config);
  const transporter = nodemailer.createTransport({
    host: config.host,
    port: config.port,
    secure: config.secure,
    auth,
    tls: { rejectUnauthorized: false },
    ...((!config.secure && config.port !== 465) && { requireTLS: true }),
  });
  await transporter.verify();
  return true;
}

// ─── Email Template CRUD ────────────────────────────────────────────

async function getTemplates(agentId) {
  const { rows } = await pool.query(
    'SELECT * FROM email_templates WHERE agent_id = $1 ORDER BY updated_at DESC',
    [agentId]
  );
  return rows;
}

async function getTemplate(id, agentId) {
  const { rows } = await pool.query(
    'SELECT * FROM email_templates WHERE id = $1 AND agent_id = $2',
    [id, agentId]
  );
  return rows[0] || null;
}

async function createTemplate(agentId, data) {
  const { name, subject, body } = data;
  const { rows } = await pool.query(
    'INSERT INTO email_templates (agent_id, name, subject, body) VALUES ($1, $2, $3, $4) RETURNING *',
    [agentId, name, subject, body]
  );
  return rows[0];
}

async function updateTemplate(id, agentId, data) {
  const { name, subject, body } = data;
  const { rows } = await pool.query(
    'UPDATE email_templates SET name=$1, subject=$2, body=$3, updated_at=NOW() WHERE id=$4 AND agent_id=$5 RETURNING *',
    [name, subject, body, id, agentId]
  );
  return rows[0] || null;
}

async function deleteTemplate(id, agentId) {
  const { rowCount } = await pool.query('DELETE FROM email_templates WHERE id = $1 AND agent_id = $2', [id, agentId]);
  return rowCount > 0;
}

// ─── Recipient Querying ─────────────────────────────────────────────

async function getRecipients({ listId, statusFilter }) {
  let clause = 'WHERE list_id = $1 AND primary_email IS NOT NULL AND primary_email != \'\'';
  const params = [listId];
  if (statusFilter && statusFilter.length > 0) {
    params.push(statusFilter);
    clause += ` AND status = ANY($${params.length})`;
  }
  const { rows } = await pool.query(
    `SELECT id, name, phone_number, primary_email, metadata FROM phone_list_entries ${clause} ORDER BY id ASC`,
    params
  );
  return rows;
}

async function countRecipients({ listId, statusFilter }) {
  let clause = 'WHERE list_id = $1 AND primary_email IS NOT NULL AND primary_email != \'\'';
  const params = [listId];
  if (statusFilter && statusFilter.length > 0) {
    params.push(statusFilter);
    clause += ` AND status = ANY($${params.length})`;
  }
  const { rows } = await pool.query(
    `SELECT COUNT(*)::int AS count FROM phone_list_entries ${clause}`,
    params
  );
  return rows[0].count;
}

// ─── Campaign CRUD ──────────────────────────────────────────────────

async function createCampaign(agentId, data) {
  const { name, subject, body, smtp_config_id, delay_ms, source_list_id, status_filter } = data;
  const { rows } = await pool.query(
    `INSERT INTO email_campaigns (agent_id, smtp_config_id, name, subject, body, delay_ms, source_list_id, status_filter)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING *`,
    [agentId, smtp_config_id, name, subject, body, delay_ms || 3000, source_list_id || null, status_filter || null]
  );
  return rows[0];
}

async function getCampaigns(agentId) {
  const { rows } = await pool.query(
    'SELECT * FROM email_campaigns WHERE agent_id = $1 ORDER BY created_at DESC',
    [agentId]
  );
  return rows;
}

async function getCampaign(id, agentId) {
  const { rows } = await pool.query(
    'SELECT * FROM email_campaigns WHERE id = $1 AND agent_id = $2',
    [id, agentId]
  );
  return rows[0] || null;
}

async function updateCampaignStatus(id, status, extra = {}) {
  const sets = ['status = $2'];
  const params = [id, status];
  if (extra.total_recipients !== undefined) {
    params.push(extra.total_recipients);
    sets.push(`total_recipients = $${params.length}`);
  }
  if (status === 'sending') sets.push('started_at = NOW()');
  if (status === 'completed' || status === 'failed') sets.push('completed_at = NOW()');
  if (extra.sent_count !== undefined) {
    params.push(extra.sent_count);
    sets.push(`sent_count = $${params.length}`);
  }
  if (extra.failed_count !== undefined) {
    params.push(extra.failed_count);
    sets.push(`failed_count = $${params.length}`);
  }
  await pool.query(`UPDATE email_campaigns SET ${sets.join(', ')} WHERE id = $1`, params);
}

async function addCampaignLog(campaignId, { email, name, status, error }) {
  await pool.query(
    `INSERT INTO email_campaign_logs (campaign_id, recipient_email, recipient_name, status, error_message, sent_at)
     VALUES ($1, $2, $3, $4, $5, ${status === 'sent' ? 'NOW()' : 'NULL'})`,
    [campaignId, email, name || null, status, error || null]
  );
}

async function getCampaignLogs(campaignId, statusFilter) {
  let clause = 'WHERE campaign_id = $1';
  const params = [campaignId];
  if (statusFilter) {
    params.push(statusFilter);
    clause += ` AND status = $${params.length}`;
  }
  const { rows } = await pool.query(
    `SELECT * FROM email_campaign_logs ${clause} ORDER BY id ASC`,
    params
  );
  return rows;
}

// ─── Uploaded Sheet Recipients ───────────────────────────────────────

async function addCampaignRecipients(campaignId, recipients) {
  if (recipients.length === 0) return;
  const values = [];
  const placeholders = [];
  let idx = 1;
  for (const r of recipients) {
    placeholders.push(`($${idx++}, $${idx++}, $${idx++})`);
    values.push(campaignId, r.email, JSON.stringify(r.data || {}));
  }
  await pool.query(
    `INSERT INTO email_campaign_recipients (campaign_id, email, data) VALUES ${placeholders.join(', ')}`,
    values
  );
}

async function getUploadedRecipients(campaignId) {
  const { rows } = await pool.query(
    'SELECT * FROM email_campaign_recipients WHERE campaign_id = $1 AND status = \'pending\' ORDER BY id ASC',
    [campaignId]
  );
  return rows;
}

async function updateUploadedRecipientStatus(id, status, error = null) {
  await pool.query(
    `UPDATE email_campaign_recipients SET status = $1, error_message = $2${status === 'sent' ? ', sent_at = NOW()' : ''} WHERE id = $3`,
    [status, error, id]
  );
}

// ─── Variable Resolution ────────────────────────────────────────────

function resolveVariables(template, entry) {
  if (!template) return '';
  let result = template;
  // Standard variables for lead-sheet entries
  result = result.replace(/\{\{name\}\}/gi, entry.name || '');
  result = result.replace(/\{\{email\}\}/gi, entry.primary_email || '');
  result = result.replace(/\{\{phone\}\}/gi, entry.phone_number || '');
  // Metadata variables
  if (entry.metadata && typeof entry.metadata === 'object') {
    for (const [key, value] of Object.entries(entry.metadata)) {
      const regex = new RegExp(`\\{\\{${key.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\}\\}`, 'gi');
      result = result.replace(regex, value || '');
    }
  }
  return result;
}

// Resolve variables from uploaded sheet data (arbitrary column keys)
function resolveUploadedVariables(template, data) {
  if (!template || !data) return template || '';
  let result = template;
  for (const [key, value] of Object.entries(data)) {
    const regex = new RegExp(`\\{\\{${key.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\}\\}`, 'gi');
    result = result.replace(regex, value != null ? String(value) : '');
  }
  return result;
}

// ─── Send Single Email ──────────────────────────────────────────────

async function sendEmail(smtpConfig, { to, cc, subject, html, attachments, headers, trackingToken }) {
  const auth = await buildSmtpAuth(smtpConfig);
  const transporter = nodemailer.createTransport({
    host: smtpConfig.host,
    port: smtpConfig.port,
    secure: smtpConfig.secure,
    auth,
    tls: { rejectUnauthorized: false },
    ...((!smtpConfig.secure && smtpConfig.port !== 465) && { requireTLS: true }),
  });
  // Inject tracking pixel and link wrappers if token provided
  const trackedHtml = trackingToken ? trackingService.injectTracking(html, trackingToken) : html;
  const mailOptions = {
    from: smtpConfig.from_name ? `"${smtpConfig.from_name}" <${smtpConfig.from_email}>` : smtpConfig.from_email,
    to,
    subject,
    html: trackedHtml,
  };
  if (cc) mailOptions.cc = cc;
  if (attachments && attachments.length > 0) {
    mailOptions.attachments = attachments;
  }
  if (headers) mailOptions.headers = headers;
  return transporter.sendMail(mailOptions);
}

// ─── Campaign Sender (called from route, emits progress via socket) ─

async function runCampaign(campaignId, agentId, io) {
  const campaign = await getCampaign(campaignId, agentId);
  if (!campaign) throw new Error('Campaign not found');

  const smtpConfig = await getSmtpConfig(campaign.smtp_config_id, agentId);
  if (!smtpConfig) throw new Error('SMTP config not found');

  // Determine recipient source: lead sheet or uploaded sheet
  const useLeadSheet = !!campaign.source_list_id;
  let recipientList;

  if (useLeadSheet) {
    const entries = await getRecipients({ listId: campaign.source_list_id, statusFilter: campaign.status_filter });
    recipientList = entries.map((e) => ({ email: e.primary_email, name: e.name, resolve: (tpl) => resolveVariables(tpl, e) }));
  } else {
    const uploaded = await getUploadedRecipients(campaignId);
    recipientList = uploaded.map((r) => ({
      id: r.id,
      email: r.email,
      name: r.data?.name || r.data?.Name || r.email,
      resolve: (tpl) => resolveUploadedVariables(tpl, r.data),
      uploaded: true,
    }));
  }

  await updateCampaignStatus(campaignId, 'sending', { total_recipients: recipientList.length });

  const room = `agent:${agentId}`;
  let sentCount = 0;
  let failedCount = 0;

  for (let i = 0; i < recipientList.length; i++) {
    const r = recipientList[i];
    const subject = r.resolve(campaign.subject);
    const html = r.resolve(campaign.body);

    try {
      // Save sent email first to get tracking token
      const saved = await saveSentEmail(agentId, { to: r.email, subject, html, smtpConfigId: smtpConfig.id });
      await sendEmail(smtpConfig, { to: r.email, subject, html, trackingToken: saved.tracking_token });
      sentCount++;
      if (r.uploaded) {
        await updateUploadedRecipientStatus(r.id, 'sent');
      }
      await addCampaignLog(campaignId, { email: r.email, name: r.name, status: 'sent' });
    } catch (err) {
      failedCount++;
      if (r.uploaded) {
        await updateUploadedRecipientStatus(r.id, 'failed', err.message);
      }
      await addCampaignLog(campaignId, { email: r.email, name: r.name, status: 'failed', error: err.message });
    }

    io.to(room).emit('email:progress', {
      campaignId,
      current: i + 1,
      total: recipientList.length,
      sentCount,
      failedCount,
      currentEmail: r.email,
    });

    if (i < recipientList.length - 1 && campaign.delay_ms > 0) {
      await new Promise((res) => setTimeout(res, campaign.delay_ms));
    }
  }

  await updateCampaignStatus(campaignId, 'completed', { sent_count: sentCount, failed_count: failedCount });
  io.to(room).emit('email:complete', { campaignId, sentCount, failedCount });
  return { sentCount, failedCount };
}

// ─── Inbox: Fetch emails via IMAP ───────────────────────────────────

async function fetchInboxEmails(agentId, targetConfigId) {
  const configs = await getSmtpConfigs(agentId);
  const imapConfigs = targetConfigId
    ? configs.filter((c) => c.id === targetConfigId && c.imap_host)
    : configs.filter((c) => c.imap_host);

  if (imapConfigs.length === 0) throw new Error('No IMAP-configured email accounts found. Update your email settings with IMAP host.');

  const results = { synced: 0, errors: [] };

  for (const config of imapConfigs) {
    try {
      const fullConfig = await getSmtpConfig(config.id, agentId);
      const imapAuth = await buildImapAuth(fullConfig);
      const client = new ImapFlow({
        host: config.imap_host,
        port: config.imap_port || 993,
        secure: config.imap_secure !== false,
        auth: imapAuth,
        logger: false,
        tls: { rejectUnauthorized: false },
      });

      await client.connect();

      try {
        // ── Inbox ──
        const { rows: lastRow } = await pool.query(
          "SELECT MAX(uid) AS max_uid FROM emails WHERE agent_id = $1 AND folder = 'inbox' AND smtp_config_id = $2",
          [agentId, config.id]
        );
        const lastUid = lastRow[0]?.max_uid || 0;

        const lock = await client.getMailboxLock('INBOX');
        try {
          const range = lastUid > 0 ? `${lastUid + 1}:*` : '1:*';
          let count = 0;
          for await (const message of client.fetch(range, { uid: true, envelope: true, source: true, flags: true })) {
            if (count >= 50) break;
            if (message.uid <= lastUid) continue;
            const parsed = await simpleParser(message.source);
            const fromAddr = parsed.from?.value?.[0]?.address || '';
            const fromName = parsed.from?.value?.[0]?.name || '';
            const toAddr = parsed.to?.value?.[0]?.address || config.from_email;
            const ccAddr = parsed.cc?.value?.map((c) => c.address).join(', ') || null;
            const msgId = parsed.messageId || null;
            const isRead = message.flags?.has('\\Seen') || false;
            const hasAttach = parsed.attachments?.length > 0 || false;
            const inReplyTo = parsed.inReplyTo || null;

            if (msgId) {
              const { rows: dup } = await pool.query('SELECT id FROM emails WHERE agent_id = $1 AND message_id = $2 LIMIT 1', [agentId, msgId]);
              if (dup.length > 0) continue;
            }

            await pool.query(
              `INSERT INTO emails (agent_id, smtp_config_id, message_id, folder, from_address, from_name, to_address, cc_address, subject, body_html, body_text, is_read, has_attachments, email_date, uid, in_reply_to)
               VALUES ($1, $2, $3, 'inbox', $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)`,
              [agentId, config.id, msgId, fromAddr, fromName, toAddr, ccAddr, parsed.subject || '(No Subject)', parsed.html || null, parsed.text || null, isRead, hasAttach, parsed.date || new Date(), message.uid, inReplyTo]
            );
            count++;
          }
          results.synced += count;
        } finally {
          lock.release();
        }

        // ── Sent folder ──
        try {
          const sentLock = await client.getMailboxLock('[Gmail]/Sent Mail').catch(() =>
            client.getMailboxLock('Sent').catch(() => client.getMailboxLock('INBOX.Sent'))
          );
          if (sentLock) {
            try {
              const { rows: lastSentRow } = await pool.query(
                "SELECT MAX(uid) AS max_uid FROM emails WHERE agent_id = $1 AND folder = 'sent' AND smtp_config_id = $2",
                [agentId, config.id]
              );
              const lastSentUid = lastSentRow[0]?.max_uid || 0;
              const sentRange = lastSentUid > 0 ? `${lastSentUid + 1}:*` : '1:*';
              let sentCount = 0;
              for await (const message of client.fetch(sentRange, { uid: true, envelope: true, source: true })) {
                if (sentCount >= 50) break;
                if (message.uid <= lastSentUid) continue;
                const parsed = await simpleParser(message.source);
                const toAddr = parsed.to?.value?.[0]?.address || '';
                const ccAddr = parsed.cc?.value?.map((c) => c.address).join(', ') || null;
                const msgId = parsed.messageId || null;
                if (msgId) {
                  const { rows: dup } = await pool.query('SELECT id FROM emails WHERE agent_id = $1 AND message_id = $2 LIMIT 1', [agentId, msgId]);
                  if (dup.length > 0) continue;
                }
                await pool.query(
                  `INSERT INTO emails (agent_id, smtp_config_id, message_id, folder, from_address, from_name, to_address, cc_address, subject, body_html, body_text, is_read, has_attachments, email_date, uid)
                   VALUES ($1, $2, $3, 'sent', $4, $5, $6, $7, $8, $9, $10, true, $11, $12, $13)`,
                  [agentId, config.id, msgId, config.from_email, config.from_name || '', toAddr, ccAddr, parsed.subject || '(No Subject)', parsed.html || null, parsed.text || null, parsed.attachments?.length > 0 || false, parsed.date || new Date(), message.uid]
                );
                sentCount++;
              }
              results.synced += sentCount;
            } finally {
              sentLock.release();
            }
          }
        } catch { /* sent folder not found */ }
      } finally {
        await client.logout();
      }
    } catch (err) {
      results.errors.push({ configId: config.id, label: config.label || config.from_email, error: err.message });
    }
  }

  if (results.synced === 0 && results.errors.length > 0) {
    throw new Error(`Sync failed: ${results.errors.map((e) => `${e.label}: ${e.error}`).join('; ')}`);
  }
  return results;
}

async function getEmails(agentId, { folder = 'all', page = 1, limit = 30, search = '', smtpConfigId }) {
  const offset = (page - 1) * limit;
  const params = [agentId];
  let folderClause = '';
  if (folder !== 'all') {
    params.push(folder);
    folderClause = ` AND folder = $${params.length}`;
  }

  let configClause = '';
  if (smtpConfigId) {
    params.push(smtpConfigId);
    configClause = ` AND smtp_config_id = $${params.length}`;
  }

  let searchClause = '';
  if (search.trim()) {
    params.push(`%${search.trim()}%`);
    const idx = params.length;
    searchClause = ` AND (subject ILIKE $${idx} OR from_address ILIKE $${idx} OR from_name ILIKE $${idx} OR to_address ILIKE $${idx})`;
  }

  const { rows } = await pool.query(
    `SELECT id, message_id, folder, from_address, from_name, to_address, cc_address, subject, is_read, has_attachments, email_date, smtp_config_id, created_at, open_count, click_count
     FROM emails
     WHERE agent_id = $1 ${folderClause} ${configClause} ${searchClause}
     ORDER BY email_date DESC
     LIMIT $${params.length + 1} OFFSET $${params.length + 2}`,
    [...params, limit, offset]
  );

  const { rows: countRows } = await pool.query(
    `SELECT COUNT(*)::int AS count FROM emails WHERE agent_id = $1 ${folderClause} ${configClause} ${searchClause}`,
    params
  );

  return { emails: rows, total: countRows[0].count, page, limit };
}

async function getEmailById(id, agentId) {
  const { rows } = await pool.query(
    'SELECT * FROM emails WHERE id = $1 AND agent_id = $2',
    [id, agentId]
  );
  return rows[0] || null;
}

async function markEmailRead(id, agentId) {
  await pool.query('UPDATE emails SET is_read = true WHERE id = $1 AND agent_id = $2', [id, agentId]);
}

async function getEmailThread(id, agentId) {
  // Find the base email first
  const base = await getEmailById(id, agentId);
  if (!base) return [];
  // Strip Re:/Fwd: prefixes to get the base subject
  const baseSubject = (base.subject || '').replace(/^(Re:\s*|Fwd:\s*)+/i, '').trim();
  if (!baseSubject) return [base];
  // Find all emails in this thread: same base subject, between same parties
  const { rows } = await pool.query(
    `SELECT * FROM emails
     WHERE agent_id = $1
       AND TRIM(LEADING FROM regexp_replace(subject, '^(Re:\\s*|Fwd:\\s*)+', '', 'i')) = $2
     ORDER BY email_date ASC`,
    [agentId, baseSubject]
  );
  return rows.length > 0 ? rows : [base];
}

async function getListColumns(listId) {
  // Get one entry to inspect metadata keys, plus fixed columns
  const { rows } = await pool.query(
    'SELECT metadata FROM phone_list_entries WHERE list_id = $1 AND metadata IS NOT NULL AND metadata::text != $2 LIMIT 1',
    [listId, '{}']
  );
  const fixedCols = ['name', 'email', 'phone'];
  if (rows.length > 0 && rows[0].metadata && typeof rows[0].metadata === 'object') {
    return [...fixedCols, ...Object.keys(rows[0].metadata)];
  }
  return fixedCols;
}

async function getUnreadCount(agentId) {
  const { rows } = await pool.query(
    "SELECT COUNT(*)::int AS count FROM emails WHERE agent_id = $1 AND folder = 'inbox' AND is_read = false",
    [agentId]
  );
  return rows[0].count;
}

// Also save sent emails locally when sending via compose/campaign
async function saveSentEmail(agentId, { to, cc, subject, html, messageId, smtpConfigId, inReplyTo }) {
  const config = smtpConfigId
    ? await getSmtpConfig(smtpConfigId, agentId)
    : (await getSmtpConfigs(agentId))[0];
  const trackingToken = trackingService.generateToken();
  const { rows } = await pool.query(
    `INSERT INTO emails (agent_id, smtp_config_id, message_id, folder, from_address, from_name, to_address, cc_address, subject, body_html, is_read, email_date, in_reply_to, tracking_token)
     VALUES ($1, $2, $3, 'sent', $4, $5, $6, $7, $8, $9, true, NOW(), $10, $11)
     RETURNING id, tracking_token`,
    [agentId, config?.id || null, messageId || null, config?.from_email || '', config?.from_name || '', to, cc || null, subject, html, inReplyTo || null, trackingToken]
  );
  return rows[0];
}

// ─── Delete Email ────────────────────────────────────────────────────

async function deleteEmail(id, agentId) {
  const { rowCount } = await pool.query('DELETE FROM emails WHERE id = $1 AND agent_id = $2', [id, agentId]);
  return rowCount > 0;
}

// ─── Reply / Forward ─────────────────────────────────────────────────

async function replyToEmail(agentId, { emailId, body, cc }) {
  const original = await getEmailById(emailId, agentId);
  if (!original) throw new Error('Email not found');

  const smtpConfig = original.smtp_config_id
    ? await getSmtpConfig(original.smtp_config_id, agentId)
    : (await getSmtpConfigs(agentId))[0];
  if (!smtpConfig) throw new Error('No SMTP config found for this email account');

  const replySubject = original.subject?.startsWith('Re: ') ? original.subject : `Re: ${original.subject || ''}`;
  const replyTo = original.folder === 'sent' ? original.to_address : original.from_address;

  const headers = {};
  if (original.message_id) {
    headers['In-Reply-To'] = original.message_id;
    headers['References'] = original.message_id;
  }

  const saved = await saveSentEmail(agentId, { to: replyTo, cc, subject: replySubject, html: body, smtpConfigId: smtpConfig.id, inReplyTo: original.message_id });
  const info = await sendEmail(smtpConfig, { to: replyTo, cc, subject: replySubject, html: body, headers, trackingToken: saved.tracking_token });
  // Update message_id after send
  await pool.query('UPDATE emails SET message_id = $1 WHERE id = $2', [info.messageId, saved.id]);
  return { success: true, message: 'Reply sent' };
}

async function forwardEmail(agentId, { emailId, to, cc, body }) {
  const original = await getEmailById(emailId, agentId);
  if (!original) throw new Error('Email not found');

  const smtpConfig = original.smtp_config_id
    ? await getSmtpConfig(original.smtp_config_id, agentId)
    : (await getSmtpConfigs(agentId))[0];
  if (!smtpConfig) throw new Error('No SMTP config found for this email account');

  const fwdSubject = original.subject?.startsWith('Fwd: ') ? original.subject : `Fwd: ${original.subject || ''}`;
  const separator = `<br/><hr/><p><strong>---------- Forwarded message ----------</strong></p><p><strong>From:</strong> ${original.from_name || ''} &lt;${original.from_address}&gt;<br/><strong>Date:</strong> ${new Date(original.email_date).toLocaleString()}<br/><strong>Subject:</strong> ${original.subject || ''}<br/><strong>To:</strong> ${original.to_address}</p>`;
  const fullBody = `${body || ''}${separator}${original.body_html || original.body_text || ''}`;

  const saved = await saveSentEmail(agentId, { to, cc, subject: fwdSubject, html: fullBody, smtpConfigId: smtpConfig.id });
  const info = await sendEmail(smtpConfig, { to, cc, subject: fwdSubject, html: fullBody, trackingToken: saved.tracking_token });
  await pool.query('UPDATE emails SET message_id = $1 WHERE id = $2', [info.messageId, saved.id]);
  return { success: true, message: 'Email forwarded' };
}

module.exports = {
  getSmtpConfigs,
  getSmtpConfig,
  saveSmtpConfig,
  updateSmtpConfig,
  deleteSmtpConfig,
  testSmtpConnection,
  getTemplates,
  getTemplate,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  getRecipients,
  countRecipients,
  createCampaign,
  getCampaigns,
  getCampaign,
  updateCampaignStatus,
  addCampaignLog,
  getCampaignLogs,
  resolveVariables,
  resolveUploadedVariables,
  addCampaignRecipients,
  getUploadedRecipients,
  updateUploadedRecipientStatus,
  sendEmail,
  runCampaign,
  fetchInboxEmails,
  getEmails,
  getEmailById,
  markEmailRead,
  getUnreadCount,
  saveSentEmail,
  deleteEmail,
  replyToEmail,
  forwardEmail,
  getEmailThread,
  getListColumns,
};
