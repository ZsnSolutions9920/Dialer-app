CREATE TABLE IF NOT EXISTS customer_minutes (
  id SERIAL PRIMARY KEY,
  customer_id INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  minutes_total INTEGER NOT NULL DEFAULT 0,
  minutes_used INTEGER NOT NULL DEFAULT 0,
  package_id VARCHAR(50),
  price_paid NUMERIC(10,2) DEFAULT 0,
  assigned_at TIMESTAMPTZ DEFAULT NOW(),
  notes TEXT
);

CREATE TABLE IF NOT EXISTS customer_numbers (
  id SERIAL PRIMARY KEY,
  customer_id INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  phone_number VARCHAR(20) NOT NULL,
  friendly_name VARCHAR(100),
  type VARCHAR(20) DEFAULT 'local',
  monthly_price NUMERIC(10,2) DEFAULT 0,
  status VARCHAR(20) DEFAULT 'active',
  assigned_at TIMESTAMPTZ DEFAULT NOW(),
  notes TEXT
);

CREATE INDEX idx_customer_minutes_customer ON customer_minutes(customer_id);
CREATE INDEX idx_customer_numbers_customer ON customer_numbers(customer_id);
