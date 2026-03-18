const pool = require('../db/pool');

async function getDashboardStats(days = 7, agentId = null) {
  const params = [days];
  let agentFilter = '';
  if (agentId) {
    agentFilter = ' AND agent_id = $2';
    params.push(agentId);
  }
  const { rows } = await pool.query(
    `SELECT
       COUNT(*)::int AS total_calls,
       COALESCE(AVG(duration_seconds) FILTER (WHERE duration_seconds > 0), 0)::int AS avg_duration,
       CASE WHEN COUNT(*) > 0
         THEN ROUND(COUNT(*) FILTER (WHERE status = 'completed' AND duration_seconds >= 60)::numeric / COUNT(*) * 100, 1)
         ELSE 0 END AS answer_rate,
       COUNT(*) FILTER (WHERE direction = 'inbound')::int AS inbound_count,
       COUNT(*) FILTER (WHERE direction = 'outbound')::int AS outbound_count
     FROM call_logs
     WHERE started_at >= NOW() - ($1 || ' days')::interval${agentFilter}`,
    params
  );
  return rows[0];
}

async function getCallVolume(days = 7, agentId = null) {
  const params = [days];
  let agentFilter = '';
  if (agentId) {
    agentFilter = ' AND agent_id = $2';
    params.push(agentId);
  }
  const { rows } = await pool.query(
    `SELECT
       started_at::date AS date,
       COUNT(*)::int AS count
     FROM call_logs
     WHERE started_at >= NOW() - ($1 || ' days')::interval${agentFilter}
     GROUP BY started_at::date
     ORDER BY date`,
    params
  );
  return rows;
}

async function getStatusBreakdown(days = 7, agentId = null) {
  const params = [days];
  let agentFilter = '';
  if (agentId) {
    agentFilter = ' AND agent_id = $2';
    params.push(agentId);
  }
  const { rows } = await pool.query(
    `SELECT
       status,
       COUNT(*)::int AS count
     FROM call_logs
     WHERE started_at >= NOW() - ($1 || ' days')::interval${agentFilter}
     GROUP BY status
     ORDER BY count DESC`,
    params
  );
  return rows;
}

async function getAgentLeaderboard(days = 7) {
  const { rows } = await pool.query(
    `SELECT
       a.id,
       a.display_name,
       COUNT(cl.id)::int AS call_count,
       COALESCE(AVG(cl.duration_seconds) FILTER (WHERE cl.duration_seconds > 0), 0)::int AS avg_duration
     FROM agents a
     LEFT JOIN call_logs cl ON cl.agent_id = a.id
       AND cl.started_at >= NOW() - ($1 || ' days')::interval
     GROUP BY a.id, a.display_name
     HAVING COUNT(cl.id) > 0
     ORDER BY call_count DESC`,
    [days]
  );
  return rows;
}

async function getTodayCallCount(agentId) {
  const { rows } = await pool.query(
    `SELECT COUNT(*)::int AS count FROM call_logs WHERE agent_id = $1 AND started_at >= CURRENT_DATE`,
    [agentId]
  );
  return rows[0].count;
}

module.exports = {
  getDashboardStats,
  getCallVolume,
  getStatusBreakdown,
  getAgentLeaderboard,
  getTodayCallCount,
};
