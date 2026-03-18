const { Pool } = require('pg');
const config = require('../config');
const logger = require('../utils/logger');

const pool = new Pool({ connectionString: config.databaseUrl });

pool.on('error', (err) => {
  logger.error(err, 'Unexpected PostgreSQL pool error');
});

module.exports = pool;
