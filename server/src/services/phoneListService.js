const pool = require('../db/pool');

async function insertEntries(client, listId, entries) {
  if (entries.length === 0) return;
  const values = [];
  const placeholders = [];
  let idx = 1;
  for (const entry of entries) {
    placeholders.push(`($${idx++}, $${idx++}, $${idx++}, $${idx++}, $${idx++})`);
    values.push(
      listId,
      entry.phone_number,
      entry.name || null,
      entry.primary_email || null,
      JSON.stringify(entry.metadata || {})
    );
  }
  await client.query(
    `INSERT INTO phone_list_entries (list_id, phone_number, name, primary_email, metadata)
     VALUES ${placeholders.join(', ')}`,
    values
  );
}

async function createList({ name, agentId, totalCount }) {
  const { rows } = await pool.query(
    `INSERT INTO phone_lists (name, agent_id, total_count)
     VALUES ($1, $2, $3)
     RETURNING *`,
    [name, agentId, totalCount]
  );
  return rows[0];
}

async function addEntries(listId, entries) {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    await insertEntries(client, listId, entries);
    await client.query('COMMIT');
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}

async function getLists(agentId) {
  const { rows } = await pool.query(
    `SELECT pl.*,
            COUNT(CASE WHEN ple.called THEN 1 END)::int AS called_count
     FROM phone_lists pl
     LEFT JOIN phone_list_entries ple ON ple.list_id = pl.id
     WHERE pl.agent_id = $1
     GROUP BY pl.id
     ORDER BY pl.created_at DESC`,
    [agentId]
  );
  return rows;
}

async function getListEntries({ listId, page = 1, limit = 20, search = '' }) {
  const offset = (page - 1) * limit;
  const params = [listId];
  let searchClause = '';

  if (search.trim()) {
    const pattern = `%${search.trim()}%`;
    const digitsOnly = search.trim().replace(/\D/g, '');
    params.push(pattern);
    const idx = params.length;
    if (digitsOnly.length > 0) {
      params.push(`%${digitsOnly}%`);
      const digitsIdx = params.length;
      searchClause = `AND (name ILIKE $${idx} OR metadata::text ILIKE $${idx} OR phone_number ILIKE $${idx} OR regexp_replace(phone_number, '[^0-9]', '', 'g') LIKE $${digitsIdx})`;
    } else {
      searchClause = `AND (name ILIKE $${idx} OR metadata::text ILIKE $${idx} OR phone_number ILIKE $${idx})`;
    }
  }

  const { rows } = await pool.query(
    `SELECT * FROM phone_list_entries
     WHERE list_id = $1 ${searchClause}
     ORDER BY
       CASE status
         WHEN 'follow_up'       THEN 1
         WHEN 'no_answer'       THEN 2
         WHEN 'pending'         THEN 3
         WHEN 'called'          THEN 4
         WHEN 'not_interested'  THEN 5
         WHEN 'do_not_contact'  THEN 6
         ELSE 7
       END,
       id ASC
     LIMIT $${params.length + 1} OFFSET $${params.length + 2}`,
    [...params, limit, offset]
  );

  const { rows: countRows } = await pool.query(
    `SELECT COUNT(*) FROM phone_list_entries WHERE list_id = $1 ${searchClause}`,
    params
  );

  return { entries: rows, total: parseInt(countRows[0].count, 10), page, limit };
}

async function markEntryCalled(entryId) {
  const { rows } = await pool.query(
    `UPDATE phone_list_entries
     SET called = true, called_at = NOW(), status = 'called'
     WHERE id = $1
     RETURNING *`,
    [entryId]
  );
  return rows[0] || null;
}

async function getEntry(entryId) {
  const { rows } = await pool.query(
    'SELECT * FROM phone_list_entries WHERE id = $1',
    [entryId]
  );
  return rows[0] || null;
}

async function updateEntryStatus(entryId, status, followUpAt = null, notes = null) {
  const effectiveFollowUp = status === 'follow_up' ? followUpAt : null;
  const { rows } = await pool.query(
    `UPDATE phone_list_entries SET status = $1, follow_up_at = $2, notes = $3 WHERE id = $4 RETURNING *`,
    [status, effectiveFollowUp, notes, entryId]
  );
  return rows[0] || null;
}

async function getFollowUps(agentId, start, end) {
  const { rows } = await pool.query(
    `SELECT ple.id, ple.name, ple.phone_number, ple.follow_up_at, ple.status,
            pl.name AS list_name
     FROM phone_list_entries ple
     JOIN phone_lists pl ON pl.id = ple.list_id
     WHERE pl.agent_id = $1
       AND ple.follow_up_at >= $2
       AND ple.follow_up_at < $3
       AND ple.status = 'follow_up'
     ORDER BY ple.follow_up_at ASC`,
    [agentId, start, end]
  );
  return rows;
}

async function getNextDialableEntry(listId, skipEntryIds = [], minId = null, forceId = null) {
  // When forceId is provided, return that exact entry first (regardless of
  // its current status) so the power dialer starts on the contact the user
  // clicked. Only 'do_not_contact' entries are excluded.
  if (forceId != null) {
    const { rows: forceRows } = await pool.query(
      `SELECT * FROM phone_list_entries
       WHERE list_id = $1 AND id = $2 AND status != 'do_not_contact'
       LIMIT 1`,
      [listId, forceId]
    );
    if (forceRows.length > 0) return forceRows[0];
    // Entry not found or is do_not_contact — fall through to normal logic
  }

  const params = [listId, ...skipEntryIds];
  const skipClause = skipEntryIds.length > 0
    ? `AND id NOT IN (${skipEntryIds.map((_, i) => `$${i + 2}`).join(', ')})`
    : '';
  let minIdClause = '';
  if (minId != null) {
    params.push(minId);
    minIdClause = `AND id >= $${params.length}`;
  }
  // When starting from a specific entry (minId set), order by id only so
  // dialing begins at the exact entry the user chose. Without minId,
  // prioritise no_answer entries so they get retried first.
  const orderClause = minId != null
    ? 'ORDER BY id ASC'
    : 'ORDER BY CASE status WHEN \'no_answer\' THEN 1 WHEN \'pending\' THEN 2 END, id ASC';
  const { rows } = await pool.query(
    `SELECT * FROM phone_list_entries
     WHERE list_id = $1
       AND status IN ('pending', 'no_answer')
       ${skipClause}
       ${minIdClause}
     ${orderClause}
     LIMIT 1`,
    params
  );
  return rows[0] || null;
}

async function getPowerDialProgress(listId) {
  const { rows } = await pool.query(
    `SELECT
       COUNT(*)::int AS total,
       COUNT(CASE WHEN status NOT IN ('pending', 'no_answer') THEN 1 END)::int AS dialed,
       COUNT(CASE WHEN status IN ('pending', 'no_answer') THEN 1 END)::int AS remaining
     FROM phone_list_entries
     WHERE list_id = $1`,
    [listId]
  );
  return rows[0];
}

async function deleteList(listId) {
  const { rowCount } = await pool.query('DELETE FROM phone_lists WHERE id = $1', [listId]);
  return rowCount > 0;
}

// Look up a contact name by phone number across all phone list entries.
// Strips non-digit characters (except leading +) for comparison.
// If agentId is provided, searches that agent's lists first.
async function findNameByPhone(phoneNumber, agentId = null) {
  // Normalize to digits only for comparison
  const digits = phoneNumber.replace(/\D/g, '');
  if (!digits) return null;

  // Try agent's lists first, then all lists
  const queries = agentId
    ? [
        {
          sql: `SELECT ple.name FROM phone_list_entries ple
                JOIN phone_lists pl ON pl.id = ple.list_id
                WHERE pl.agent_id = $1
                  AND regexp_replace(ple.phone_number, '\\D', '', 'g') = $2
                  AND ple.name IS NOT NULL AND ple.name != ''
                ORDER BY ple.called_at DESC NULLS LAST, ple.id DESC
                LIMIT 1`,
          params: [agentId, digits],
        },
        {
          sql: `SELECT ple.name FROM phone_list_entries ple
                WHERE regexp_replace(ple.phone_number, '\\D', '', 'g') = $1
                  AND ple.name IS NOT NULL AND ple.name != ''
                ORDER BY ple.called_at DESC NULLS LAST, ple.id DESC
                LIMIT 1`,
          params: [digits],
        },
      ]
    : [
        {
          sql: `SELECT ple.name FROM phone_list_entries ple
                WHERE regexp_replace(ple.phone_number, '\\D', '', 'g') = $1
                  AND ple.name IS NOT NULL AND ple.name != ''
                ORDER BY ple.called_at DESC NULLS LAST, ple.id DESC
                LIMIT 1`,
          params: [digits],
        },
      ];

  for (const q of queries) {
    const { rows } = await pool.query(q.sql, q.params);
    if (rows.length > 0) return rows[0].name;
  }
  return null;
}

module.exports = {
  createList,
  addEntries,
  getLists,
  getListEntries,
  getEntry,
  markEntryCalled,
  updateEntryStatus,
  getFollowUps,
  getNextDialableEntry,
  getPowerDialProgress,
  deleteList,
  findNameByPhone,
};
