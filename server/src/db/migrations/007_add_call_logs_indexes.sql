CREATE INDEX IF NOT EXISTS idx_call_logs_started_at ON call_logs (started_at DESC);
CREATE INDEX IF NOT EXISTS idx_call_logs_direction ON call_logs (direction);
CREATE INDEX IF NOT EXISTS idx_call_logs_status ON call_logs (status);
CREATE INDEX IF NOT EXISTS idx_call_logs_agent_id ON call_logs (agent_id);
