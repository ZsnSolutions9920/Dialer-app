ALTER TABLE phone_list_entries ADD COLUMN IF NOT EXISTS primary_email VARCHAR(255);
ALTER TABLE phone_list_entries ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}';
