CREATE TABLE IF NOT EXISTS call_logs (
  id SERIAL PRIMARY KEY,
  call_sid VARCHAR(64) UNIQUE,
  conference_sid VARCHAR(64),
  direction VARCHAR(10) NOT NULL,
  agent_id INTEGER REFERENCES agents(id),
  from_number VARCHAR(20),
  to_number VARCHAR(20),
  status VARCHAR(20) NOT NULL DEFAULT 'initiated',
  duration_seconds INTEGER,
  transferred_to INTEGER REFERENCES agents(id),
  transferred_from INTEGER REFERENCES agents(id),
  started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  ended_at TIMESTAMPTZ
);
