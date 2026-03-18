ALTER TABLE phone_list_entries ADD COLUMN IF NOT EXISTS follow_up_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_entries_follow_up_at
  ON phone_list_entries (follow_up_at)
  WHERE follow_up_at IS NOT NULL;
