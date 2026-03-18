-- Store uploaded recipients for campaigns that don't use an existing lead sheet
CREATE TABLE IF NOT EXISTS email_campaign_recipients (
  id SERIAL PRIMARY KEY,
  campaign_id INTEGER NOT NULL REFERENCES email_campaigns(id) ON DELETE CASCADE,
  email VARCHAR(255) NOT NULL,
  data JSONB NOT NULL DEFAULT '{}',
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  error_message TEXT,
  sent_at TIMESTAMPTZ
);

CREATE INDEX idx_ecr_campaign ON email_campaign_recipients(campaign_id);
