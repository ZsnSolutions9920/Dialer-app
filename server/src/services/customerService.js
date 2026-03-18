const pool = require('../db/pool');

module.exports = {
  async findByEmail(email) {
    const { rows } = await pool.query('SELECT * FROM customers WHERE email = $1', [email.toLowerCase()]);
    return rows[0] || null;
  },

  async findById(id) {
    const { rows } = await pool.query('SELECT * FROM customers WHERE id = $1', [id]);
    return rows[0] || null;
  },

  async create(email, passwordHash, name) {
    const { rows } = await pool.query(
      'INSERT INTO customers (email, password_hash, name) VALUES ($1, $2, $3) RETURNING *',
      [email.toLowerCase(), passwordHash, name || null]
    );
    return rows[0];
  },

  async getAll({ page = 1, limit = 50, search } = {}) {
    const offset = (page - 1) * limit;
    let where = '';
    const params = [];

    if (search) {
      params.push(`%${search}%`);
      where = `WHERE email ILIKE $${params.length} OR name ILIKE $${params.length}`;
    }

    const countResult = await pool.query(`SELECT COUNT(*) FROM customers ${where}`, params);
    const total = parseInt(countResult.rows[0].count, 10);

    params.push(limit, offset);
    const { rows } = await pool.query(
      `SELECT id, email, name, status, package, created_at, updated_at
       FROM customers ${where}
       ORDER BY created_at DESC
       LIMIT $${params.length - 1} OFFSET $${params.length}`,
      params
    );

    return { customers: rows, total, page, limit };
  },

  async updateStatus(id, status) {
    const { rows } = await pool.query(
      'UPDATE customers SET status = $1, updated_at = NOW() WHERE id = $2 RETURNING *',
      [status, id]
    );
    return rows[0];
  },

  async updatePackage(id, pkg) {
    const { rows } = await pool.query(
      'UPDATE customers SET package = $1, updated_at = NOW() WHERE id = $2 RETURNING *',
      [pkg, id]
    );
    return rows[0];
  },

  async deleteCustomer(id) {
    await pool.query('DELETE FROM customers WHERE id = $1', [id]);
  },

  async getStats() {
    const { rows } = await pool.query(`
      SELECT
        COUNT(*) AS total,
        COUNT(*) FILTER (WHERE status = 'active') AS active,
        COUNT(*) FILTER (WHERE status = 'suspended') AS suspended,
        COUNT(*) FILTER (WHERE created_at >= NOW() - INTERVAL '7 days') AS new_this_week
      FROM customers
    `);
    return rows[0];
  },
};
