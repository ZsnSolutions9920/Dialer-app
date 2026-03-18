-- Add IMAP fields to smtp_configs
ALTER TABLE smtp_configs ADD COLUMN IF NOT EXISTS imap_host VARCHAR(255);
ALTER TABLE smtp_configs ADD COLUMN IF NOT EXISTS imap_port INTEGER DEFAULT 993;
ALTER TABLE smtp_configs ADD COLUMN IF NOT EXISTS imap_secure BOOLEAN DEFAULT true;

-- Unified emails table for sent and received
CREATE TABLE IF NOT EXISTS emails (
  id SERIAL PRIMARY KEY,
  agent_id INTEGER NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
  message_id VARCHAR(500),
  folder VARCHAR(50) NOT NULL DEFAULT 'inbox',
  from_address VARCHAR(255) NOT NULL,
  from_name VARCHAR(255),
  to_address VARCHAR(255) NOT NULL,
  subject VARCHAR(1000),
  body_html TEXT,
  body_text TEXT,
  is_read BOOLEAN NOT NULL DEFAULT false,
  has_attachments BOOLEAN NOT NULL DEFAULT false,
  email_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  uid INTEGER,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_emails_agent_folder ON emails(agent_id, folder);
CREATE INDEX idx_emails_agent_date ON emails(agent_id, email_date DESC);
CREATE INDEX idx_emails_message_id ON emails(message_id);
