const pool = require('../db/pool');

async function getContacts({ agentId, search, page = 1, limit = 50 }) {
  const conditions = [];
  const values = [];
  let idx = 1;

  if (agentId) {
    conditions.push(`c.agent_id = $${idx++}`);
    values.push(agentId);
  }

  if (search) {
    conditions.push(`(c.name ILIKE $${idx} OR c.phone_number ILIKE $${idx} OR c.company ILIKE $${idx})`);
    values.push(`%${search}%`);
    idx++;
  }

  const where = conditions.length > 0 ? 'WHERE ' + conditions.join(' AND ') : '';
  const offset = (page - 1) * limit;

  const { rows } = await pool.query(
    `SELECT c.*, a.display_name as agent_name
     FROM contacts c
     LEFT JOIN agents a ON c.agent_id = a.id
     ${where}
     ORDER BY c.is_favorite DESC, c.created_at DESC
     LIMIT $${idx} OFFSET $${idx + 1}`,
    [...values, limit, offset]
  );

  const { rows: countRows } = await pool.query(
    `SELECT COUNT(*) FROM contacts c ${where}`,
    values
  );

  return { contacts: rows, total: parseInt(countRows[0].count, 10), page, limit };
}

async function getContactById(id) {
  const { rows } = await pool.query('SELECT * FROM contacts WHERE id = $1', [id]);
  return rows[0] || null;
}

async function getContactByPhone(phone) {
  const { rows } = await pool.query('SELECT * FROM contacts WHERE phone_number = $1', [phone]);
  return rows[0] || null;
}

async function createContact({ name, phoneNumber, email, company, notes, agentId }) {
  const { rows } = await pool.query(
    `INSERT INTO contacts (name, phone_number, email, company, notes, agent_id)
     VALUES ($1, $2, $3, $4, $5, $6)
     RETURNING *`,
    [name, phoneNumber, email || null, company || null, notes || null, agentId || null]
  );
  return rows[0];
}

async function updateContact(id, { name, phoneNumber, email, company, notes }) {
  const { rows } = await pool.query(
    `UPDATE contacts
     SET name = COALESCE($1, name),
         phone_number = COALESCE($2, phone_number),
         email = $3,
         company = $4,
         notes = $5,
         updated_at = NOW()
     WHERE id = $6
     RETURNING *`,
    [name, phoneNumber, email, company, notes, id]
  );
  return rows[0] || null;
}

async function toggleFavorite(id) {
  const { rows } = await pool.query(
    `UPDATE contacts SET is_favorite = NOT is_favorite, updated_at = NOW() WHERE id = $1 RETURNING *`,
    [id]
  );
  return rows[0] || null;
}

async function deleteContact(id) {
  const { rowCount } = await pool.query('DELETE FROM contacts WHERE id = $1', [id]);
  return rowCount > 0;
}

module.exports = {
  getContacts,
  getContactById,
  getContactByPhone,
  createContact,
  updateContact,
  toggleFavorite,
  deleteContact,
};
