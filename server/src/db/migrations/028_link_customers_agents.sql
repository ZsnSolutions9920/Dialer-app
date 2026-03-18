-- Link agents to customers (one customer can have one agent profile for calling)
ALTER TABLE agents ADD COLUMN IF NOT EXISTS customer_id INTEGER REFERENCES customers(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_agents_customer ON agents(customer_id);
