const twilioClient = require('./twilioClient');
const pool = require('../db/pool');
const logger = require('../utils/logger');

async function createActiveCall({ callSid, conferenceSid, conferenceName, agentId, direction, from, to, status }) {
  const { rows } = await pool.query(
    `INSERT INTO active_calls (call_sid, conference_sid, conference_name, agent_id, direction, from_number, to_number, status)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
     ON CONFLICT (call_sid) DO UPDATE SET
       conference_sid = COALESCE(EXCLUDED.conference_sid, active_calls.conference_sid),
       conference_name = COALESCE(EXCLUDED.conference_name, active_calls.conference_name),
       status = EXCLUDED.status
     RETURNING *`,
    [callSid, conferenceSid, conferenceName, agentId, direction, from, to, status || 'initiated']
  );
  return rows[0];
}

async function getActiveCallByAgent(agentId) {
  const { rows } = await pool.query('SELECT * FROM active_calls WHERE agent_id = $1', [agentId]);
  return rows[0] || null;
}

async function getActiveCallBySid(callSid) {
  const { rows } = await pool.query('SELECT * FROM active_calls WHERE call_sid = $1', [callSid]);
  return rows[0] || null;
}

async function getActiveCallByConference(conferenceName) {
  const { rows } = await pool.query('SELECT * FROM active_calls WHERE conference_name = $1 LIMIT 1', [conferenceName]);
  return rows[0] || null;
}

async function getAllActiveCalls() {
  const { rows } = await pool.query(
    `SELECT ac.*, a.display_name as agent_name
     FROM active_calls ac
     JOIN agents a ON ac.agent_id = a.id
     WHERE ac.status = 'in-progress'`
  );
  return rows;
}

async function removeActiveCall(callSid) {
  await pool.query('DELETE FROM active_calls WHERE call_sid = $1', [callSid]);
}

async function removeActiveCallsByConference(conferenceName) {
  await pool.query('DELETE FROM active_calls WHERE conference_name = $1', [conferenceName]);
}

async function createCallLog({ callSid, conferenceSid, direction, agentId, from, to, status }) {
  const { rows } = await pool.query(
    `INSERT INTO call_logs (call_sid, conference_sid, direction, agent_id, from_number, to_number, status)
     VALUES ($1, $2, $3, $4, $5, $6, $7)
     ON CONFLICT (call_sid) DO UPDATE SET
       conference_sid = COALESCE(EXCLUDED.conference_sid, call_logs.conference_sid),
       status = EXCLUDED.status
     RETURNING *`,
    [callSid, conferenceSid, direction, agentId, from, to, status || 'initiated']
  );
  return rows[0];
}

async function updateCallLog(callSid, updates) {
  const fields = [];
  const values = [];
  let idx = 1;

  for (const [key, val] of Object.entries(updates)) {
    const column = key.replace(/([A-Z])/g, '_$1').toLowerCase();
    fields.push(`${column} = $${idx}`);
    values.push(val);
    idx++;
  }
  values.push(callSid);

  if (fields.length === 0) return null;

  const { rows } = await pool.query(
    `UPDATE call_logs SET ${fields.join(', ')} WHERE call_sid = $${idx} RETURNING *`,
    values
  );
  return rows[0] || null;
}

async function getCallLogs({ page = 1, limit = 20, search, direction, status, disposition, dateFrom, dateTo, agentId }) {
  const conditions = [];
  const values = [];
  let idx = 1;

  if (search) {
    conditions.push(`(cl.from_number ILIKE $${idx} OR cl.to_number ILIKE $${idx} OR a.display_name ILIKE $${idx})`);
    values.push(`%${search}%`);
    idx++;
  }
  if (direction) {
    conditions.push(`cl.direction = $${idx++}`);
    values.push(direction);
  }
  if (status) {
    conditions.push(`cl.status = $${idx++}`);
    values.push(status);
  }
  if (disposition) {
    conditions.push(`cl.disposition = $${idx++}`);
    values.push(disposition);
  }
  if (dateFrom) {
    conditions.push(`cl.started_at >= $${idx++}`);
    values.push(dateFrom);
  }
  if (dateTo) {
    conditions.push(`cl.started_at <= $${idx++}`);
    values.push(dateTo);
  }
  if (agentId) {
    conditions.push(`cl.agent_id = $${idx++}`);
    values.push(agentId);
  }

  const where = conditions.length > 0 ? 'WHERE ' + conditions.join(' AND ') : '';
  const offset = (page - 1) * limit;

  const { rows } = await pool.query(
    `SELECT cl.*, a.display_name as agent_name,
            con.name as contact_name,
            ple.primary_email as lead_email,
            ple.name as lead_name,
            ple.metadata as lead_metadata
     FROM call_logs cl
     LEFT JOIN agents a ON cl.agent_id = a.id
     LEFT JOIN contacts con ON (cl.from_number = con.phone_number OR cl.to_number = con.phone_number)
     LEFT JOIN LATERAL (
       SELECT primary_email, name, metadata FROM phone_list_entries
       WHERE phone_number = cl.to_number OR phone_number = cl.from_number
       ORDER BY id DESC LIMIT 1
     ) ple ON true
     ${where}
     ORDER BY cl.started_at DESC
     LIMIT $${idx} OFFSET $${idx + 1}`,
    [...values, limit, offset]
  );
  const { rows: countRows } = await pool.query(
    `SELECT COUNT(*) FROM call_logs cl LEFT JOIN agents a ON cl.agent_id = a.id ${where}`,
    values
  );
  return { calls: rows, total: parseInt(countRows[0].count, 10), page, limit };
}

async function updateCallNotes(callId, { notes, disposition }) {
  const { rows } = await pool.query(
    `UPDATE call_logs SET notes = $1, disposition = $2 WHERE id = $3 RETURNING *`,
    [notes, disposition, callId]
  );
  return rows[0] || null;
}

async function getCallLogsForExport({ search, direction, status, disposition, dateFrom, dateTo, agentId }) {
  const conditions = [];
  const values = [];
  let idx = 1;

  if (search) {
    conditions.push(`(cl.from_number ILIKE $${idx} OR cl.to_number ILIKE $${idx} OR a.display_name ILIKE $${idx})`);
    values.push(`%${search}%`);
    idx++;
  }
  if (direction) {
    conditions.push(`cl.direction = $${idx++}`);
    values.push(direction);
  }
  if (status) {
    conditions.push(`cl.status = $${idx++}`);
    values.push(status);
  }
  if (disposition) {
    conditions.push(`cl.disposition = $${idx++}`);
    values.push(disposition);
  }
  if (dateFrom) {
    conditions.push(`cl.started_at >= $${idx++}`);
    values.push(dateFrom);
  }
  if (dateTo) {
    conditions.push(`cl.started_at <= $${idx++}`);
    values.push(dateTo);
  }
  if (agentId) {
    conditions.push(`cl.agent_id = $${idx++}`);
    values.push(agentId);
  }

  const where = conditions.length > 0 ? 'WHERE ' + conditions.join(' AND ') : '';

  const { rows } = await pool.query(
    `SELECT cl.*, a.display_name as agent_name,
            con.name as contact_name,
            ple.primary_email as lead_email,
            ple.name as lead_name,
            ple.metadata as lead_metadata
     FROM call_logs cl
     LEFT JOIN agents a ON cl.agent_id = a.id
     LEFT JOIN contacts con ON (cl.from_number = con.phone_number OR cl.to_number = con.phone_number)
     LEFT JOIN LATERAL (
       SELECT primary_email, name, metadata FROM phone_list_entries
       WHERE phone_number = cl.to_number OR phone_number = cl.from_number
       ORDER BY id DESC LIMIT 1
     ) ple ON true
     ${where}
     ORDER BY cl.started_at DESC`,
    values
  );
  return rows;
}

async function toggleHold(conferenceSid, callSid, hold) {
  const conferences = await twilioClient.conferences(conferenceSid).participants.list();
  // Find the external participant (non-client)
  for (const p of conferences) {
    if (p.callSid === callSid) {
      await twilioClient.conferences(conferenceSid).participants(callSid).update({ hold });
      logger.info({ conferenceSid, callSid, hold }, 'Toggled hold');
      return { held: hold };
    }
  }
  throw new Error('Participant not found in conference');
}

async function holdParticipant(conferenceSid, participantCallSid, hold) {
  await twilioClient.conferences(conferenceSid).participants(participantCallSid).update({
    hold,
    holdUrl: hold ? `${require('../config').serverBaseUrl}/api/twilio/hold-music` : undefined,
  });
  return { held: hold };
}

async function hangupConference(conferenceName) {
  // Find the conference by friendly name
  const conferences = await twilioClient.conferences.list({
    friendlyName: conferenceName,
    status: 'in-progress',
  });

  for (const conf of conferences) {
    const participants = await twilioClient.conferences(conf.sid).participants.list();
    for (const p of participants) {
      await twilioClient.conferences(conf.sid).participants(p.callSid).remove();
    }
    logger.info({ conferenceName, conferenceSid: conf.sid }, 'Conference ended');
  }
}

async function addParticipantToConference(conferenceName, to, from) {
  const participant = await twilioClient.conferences(conferenceName)
    .participants
    .create({
      to,
      from,
      earlyMedia: true,
      endConferenceOnExit: false,
    });
  return participant;
}

async function purgeOldCallLogs() {
  const { rows: countRows } = await pool.query('SELECT COUNT(*) FROM call_logs');
  const totalCount = parseInt(countRows[0].count, 10);

  if (totalCount === 0) {
    return { deletedCount: 0, remainingCount: 0 };
  }

  const keepCount = Math.ceil(totalCount * 0.10);

  const { rowCount } = await pool.query(
    `DELETE FROM call_logs
     WHERE id NOT IN (
       SELECT id FROM call_logs ORDER BY started_at DESC LIMIT $1
     )`,
    [keepCount]
  );

  return { deletedCount: rowCount, remainingCount: totalCount - rowCount };
}

async function deleteCallLog(callId) {
  const { rows } = await pool.query(
    'DELETE FROM call_logs WHERE id = $1 RETURNING id',
    [callId]
  );
  return rows[0] || null;
}

module.exports = {
  createActiveCall,
  getActiveCallByAgent,
  getActiveCallBySid,
  getActiveCallByConference,
  getAllActiveCalls,
  removeActiveCall,
  removeActiveCallsByConference,
  createCallLog,
  updateCallLog,
  getCallLogs,
  updateCallNotes,
  getCallLogsForExport,
  deleteCallLog,
  purgeOldCallLogs,
  toggleHold,
  holdParticipant,
  hangupConference,
  addParticipantToConference,
};
