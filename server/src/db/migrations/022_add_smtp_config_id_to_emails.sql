-- Add smtp_config_id to link emails to their SMTP account
ALTER TABLE emails ADD COLUMN IF NOT EXISTS smtp_config_id INTEGER REFERENCES smtp_configs(id) ON DELETE SET NULL;

-- Add cc_address for reply/forward CC tracking
ALTER TABLE emails ADD COLUMN IF NOT EXISTS cc_address TEXT;

-- Add in_reply_to for threading
ALTER TABLE emails ADD COLUMN IF NOT EXISTS in_reply_to VARCHAR(500);

-- Index for filtering by smtp account
CREATE INDEX IF NOT EXISTS idx_emails_smtp_config ON emails(smtp_config_id);

-- Backfill: assign existing emails to the agent's first SMTP config
UPDATE emails e
SET smtp_config_id = (
  SELECT s.id FROM smtp_configs s
  WHERE s.agent_id = e.agent_id
  ORDER BY s.is_default DESC, s.created_at ASC
  LIMIT 1
)
WHERE e.smtp_config_id IS NULL;
