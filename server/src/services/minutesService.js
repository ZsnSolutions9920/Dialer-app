const pool = require('../db/pool');

module.exports = {
  // Get remaining minutes for a customer
  async getRemaining(customerId) {
    const { rows } = await pool.query(
      `SELECT COALESCE(SUM(minutes_total) - SUM(minutes_used), 0) AS remaining
       FROM customer_minutes WHERE customer_id = $1`,
      [customerId]
    );
    return parseInt(rows[0].remaining, 10) || 0;
  },

  // Deduct minutes after a call (spread across allocations FIFO)
  async deduct(customerId, minutes) {
    let remaining = minutes;

    // Get allocations with available minutes, oldest first
    const { rows } = await pool.query(
      `SELECT id, minutes_total, minutes_used
       FROM customer_minutes
       WHERE customer_id = $1 AND minutes_used < minutes_total
       ORDER BY assigned_at ASC`,
      [customerId]
    );

    for (const alloc of rows) {
      if (remaining <= 0) break;
      const available = alloc.minutes_total - alloc.minutes_used;
      const deductAmount = Math.min(available, remaining);

      await pool.query(
        'UPDATE customer_minutes SET minutes_used = minutes_used + $1 WHERE id = $2',
        [deductAmount, alloc.id]
      );
      remaining -= deductAmount;
    }

    return minutes - remaining; // actual deducted
  },

  // Find customer_id from agent_id
  async getCustomerIdForAgent(agentId) {
    const { rows } = await pool.query(
      'SELECT customer_id FROM agents WHERE id = $1',
      [agentId]
    );
    return rows[0]?.customer_id || null;
  },
};
