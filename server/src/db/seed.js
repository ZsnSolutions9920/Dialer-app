require('dotenv').config();
const bcrypt = require('bcryptjs');
const { Pool } = require('pg');

const pool = new Pool({ connectionString: process.env.DATABASE_URL });

const agents = [
  { username: 'agent1', password: 'password1', display_name: 'Alice Johnson', twilio_identity: 'agent_alice', twilio_phone_number: '+16188223636' }, //Azekile
  { username: 'agent2', password: 'password2', display_name: 'Bob Smith', twilio_identity: 'agent_bob', twilio_phone_number: '+17372325335' }, // Hassan
  { username: 'agent3', password: 'password3', display_name: 'Carol Davis', twilio_identity: 'agent_carol', twilio_phone_number: '+19404779335'}, 
  { username: 'agent4', password: 'password4', display_name: 'Dave Wilson', twilio_identity: 'agent_dave', twilio_phone_number: '+17406196969' }, // Jared
  { username: 'agent5', password: 'password5', display_name: 'Eve Martinez', twilio_identity: 'agent_eve', twilio_phone_number: '+17209731230' }, // Salim
];

async function seed() {
  const client = await pool.connect();
  try {
    for (const a of agents) {
      const hash = await bcrypt.hash(a.password, 10);
      await client.query(
        `INSERT INTO agents (username, password_hash, display_name, twilio_identity, twilio_phone_number)
         VALUES ($1, $2, $3, $4, $5)
         ON CONFLICT (username) DO UPDATE SET twilio_phone_number = EXCLUDED.twilio_phone_number`,
        [a.username, hash, a.display_name, a.twilio_identity, a.twilio_phone_number]
      );
      console.log(`  seeded: ${a.username}`);
    }
    console.log('Seed complete.');
  } finally {
    client.release();
    await pool.end();
  }
}

seed();
