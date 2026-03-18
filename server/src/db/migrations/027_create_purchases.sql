CREATE TABLE IF NOT EXISTS purchases (
  id SERIAL PRIMARY KEY,
  customer_id INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  type VARCHAR(20) NOT NULL,
  item_id VARCHAR(50),
  item_label VARCHAR(255),
  amount NUMERIC(10,2) NOT NULL DEFAULT 0,
  currency VARCHAR(10) DEFAULT 'usd',
  status VARCHAR(20) DEFAULT 'pending',
  stripe_session_id VARCHAR(255),
  stripe_payment_id VARCHAR(255),
  mock BOOLEAN DEFAULT false,
  metadata JSONB DEFAULT '{}',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  completed_at TIMESTAMPTZ
);

CREATE INDEX idx_purchases_customer ON purchases(customer_id);
CREATE INDEX idx_purchases_stripe ON purchases(stripe_session_id);
