const pool = require('../db/pool');

async function findByUsername(username) {
  const { rows } = await pool.query('SELECT * FROM agents WHERE username = $1', [username]);
  return rows[0] || null;
}

async function findById(id) {
  const { rows } = await pool.query('SELECT * FROM agents WHERE id = $1', [id]);
  return rows[0] || null;
}

async function findByIdentity(identity) {
  const { rows } = await pool.query('SELECT * FROM agents WHERE twilio_identity = $1', [identity]);
  return rows[0] || null;
}

async function findByPhoneNumber(phoneNumber) {
  const { rows } = await pool.query('SELECT * FROM agents WHERE twilio_phone_number = $1', [phoneNumber]);
  return rows[0] || null;
}

async function listAll() {
  const { rows } = await pool.query(
    'SELECT id, username, display_name, twilio_identity, twilio_phone_number, status FROM agents ORDER BY id'
  );
  return rows;
}

async function listAvailable() {
  const { rows } = await pool.query(
    "SELECT * FROM agents WHERE status = 'available' ORDER BY id"
  );
  return rows;
}

async function updateStatus(id, status) {
  const { rows } = await pool.query(
    'UPDATE agents SET status = $1, updated_at = NOW() WHERE id = $2 RETURNING id, username, display_name, twilio_identity, twilio_phone_number, status',
    [status, id]
  );
  return rows[0] || null;
}

async function getProfile(id) {
  const { rows } = await pool.query(
    'SELECT id, username, display_name, email, phone, department, bio, twilio_identity, twilio_phone_number, status, created_at FROM agents WHERE id = $1',
    [id]
  );
  return rows[0] || null;
}

async function updateProfile(id, { displayName, email, phone, department, bio }) {
  const { rows } = await pool.query(
    `UPDATE agents SET
       display_name = COALESCE($2, display_name),
       email = COALESCE($3, email),
       phone = COALESCE($4, phone),
       department = COALESCE($5, department),
       bio = COALESCE($6, bio),
       updated_at = NOW()
     WHERE id = $1
     RETURNING id, username, display_name, email, phone, department, bio, twilio_identity, twilio_phone_number, status, created_at`,
    [id, displayName, email, phone, department, bio]
  );
  return rows[0] || null;
}

module.exports = { findByUsername, findById, findByIdentity, findByPhoneNumber, listAll, listAvailable, updateStatus, getProfile, updateProfile };
