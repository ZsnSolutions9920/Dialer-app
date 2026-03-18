CREATE TABLE IF NOT EXISTS active_calls (
  id SERIAL PRIMARY KEY,
  call_sid VARCHAR(64) UNIQUE NOT NULL,
  conference_sid VARCHAR(64),
  conference_name VARCHAR(128),
  agent_id INTEGER REFERENCES agents(id),
  direction VARCHAR(10) NOT NULL,
  from_number VARCHAR(20),
  to_number VARCHAR(20),
  status VARCHAR(20) NOT NULL DEFAULT 'initiated'
);
