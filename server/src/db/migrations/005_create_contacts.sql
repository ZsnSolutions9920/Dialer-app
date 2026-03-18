CREATE TABLE IF NOT EXISTS contacts (
  id            SERIAL PRIMARY KEY,
  name          VARCHAR(200) NOT NULL,
  phone_number  VARCHAR(30) NOT NULL,
  email         VARCHAR(255),
  company       VARCHAR(200),
  notes         TEXT,
  agent_id      INTEGER REFERENCES agents(id) ON DELETE SET NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_contacts_phone_number ON contacts (phone_number);
CREATE INDEX IF NOT EXISTS idx_contacts_agent_id ON contacts (agent_id);
