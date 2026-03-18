CREATE TABLE IF NOT EXISTS phone_lists (
  id          SERIAL PRIMARY KEY,
  name        VARCHAR(200) NOT NULL,
  agent_id    INTEGER REFERENCES agents(id) ON DELETE SET NULL,
  total_count INTEGER NOT NULL DEFAULT 0,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS phone_list_entries (
  id            SERIAL PRIMARY KEY,
  list_id       INTEGER REFERENCES phone_lists(id) ON DELETE CASCADE,
  phone_number  VARCHAR(30) NOT NULL,
  name          VARCHAR(200),
  called        BOOLEAN NOT NULL DEFAULT FALSE,
  called_at     TIMESTAMPTZ,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_phone_list_entries_list_id ON phone_list_entries (list_id);
