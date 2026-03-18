-- Email tracking events table
CREATE TABLE IF NOT EXISTS email_tracking_events (
  id SERIAL PRIMARY KEY,
  agent_id INTEGER NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
  email_id INTEGER REFERENCES emails(id) ON DELETE SET NULL,
  campaign_id INTEGER REFERENCES email_campaigns(id) ON DELETE SET NULL,
  recipient_email VARCHAR(255) NOT NULL,
  event_type VARCHAR(20) NOT NULL, -- 'open' or 'click'
  link_url TEXT,
  ip_address VARCHAR(45),
  user_agent TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tracking_agent ON email_tracking_events(agent_id, created_at DESC);
CREATE INDEX idx_tracking_email ON email_tracking_events(email_id);
CREATE INDEX idx_tracking_campaign ON email_tracking_events(campaign_id);

-- Add tracking columns to emails table
ALTER TABLE emails ADD COLUMN IF NOT EXISTS open_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE emails ADD COLUMN IF NOT EXISTS click_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE emails ADD COLUMN IF NOT EXISTS first_opened_at TIMESTAMPTZ;
ALTER TABLE emails ADD COLUMN IF NOT EXISTS last_opened_at TIMESTAMPTZ;
ALTER TABLE emails ADD COLUMN IF NOT EXISTS first_clicked_at TIMESTAMPTZ;

-- Add tracking token to emails (used in tracking pixel URLs)
ALTER TABLE emails ADD COLUMN IF NOT EXISTS tracking_token VARCHAR(64);
CREATE UNIQUE INDEX IF NOT EXISTS idx_emails_tracking_token ON emails(tracking_token) WHERE tracking_token IS NOT NULL;
