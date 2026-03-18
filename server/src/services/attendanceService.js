const pool = require('../db/pool');

async function clockIn(agentId) {
  // Reject if there's already an open session
  const open = await getCurrentSession(agentId);
  if (open) {
    throw new Error('Already clocked in');
  }

  const { rows } = await pool.query(
    'INSERT INTO attendance_logs (agent_id) VALUES ($1) RETURNING *',
    [agentId]
  );
  return rows[0];
}

async function clockOut(agentId) {
  const open = await getCurrentSession(agentId);
  if (!open) {
    throw new Error('No open session to clock out');
  }

  const { rows } = await pool.query(
    `UPDATE attendance_logs
     SET clock_out = NOW(),
         duration_seconds = EXTRACT(EPOCH FROM (NOW() - clock_in))::INTEGER
     WHERE id = $1
     RETURNING *`,
    [open.id]
  );
  return rows[0];
}

async function getCurrentSession(agentId) {
  const { rows } = await pool.query(
    'SELECT * FROM attendance_logs WHERE agent_id = $1 AND clock_out IS NULL ORDER BY clock_in DESC LIMIT 1',
    [agentId]
  );
  return rows[0] || null;
}

async function getHistory(agentId, { limit = 50, offset = 0 } = {}) {
  const { rows } = await pool.query(
    'SELECT * FROM attendance_logs WHERE agent_id = $1 ORDER BY clock_in DESC LIMIT $2 OFFSET $3',
    [agentId, limit, offset]
  );
  const countResult = await pool.query(
    'SELECT COUNT(*) FROM attendance_logs WHERE agent_id = $1',
    [agentId]
  );
  return { entries: rows, total: parseInt(countResult.rows[0].count, 10) };
}

module.exports = { clockIn, clockOut, getCurrentSession, getHistory };
