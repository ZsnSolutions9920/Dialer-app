-- Add OAuth fields to smtp_configs for Google/OAuth email accounts
ALTER TABLE smtp_configs ADD COLUMN IF NOT EXISTS auth_type VARCHAR(20) NOT NULL DEFAULT 'password'; -- 'password' or 'oauth'
ALTER TABLE smtp_configs ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(20); -- 'google'
ALTER TABLE smtp_configs ADD COLUMN IF NOT EXISTS oauth_access_token TEXT;
ALTER TABLE smtp_configs ADD COLUMN IF NOT EXISTS oauth_refresh_token TEXT;
ALTER TABLE smtp_configs ADD COLUMN IF NOT EXISTS oauth_token_expiry TIMESTAMPTZ;

-- Make password nullable for OAuth accounts (they use tokens instead)
ALTER TABLE smtp_configs ALTER COLUMN password DROP NOT NULL;
