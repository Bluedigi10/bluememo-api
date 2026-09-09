ALTER TABLE channel_accounts ADD COLUMN revoked_at TIMESTAMPTZ;
ALTER TABLE channel_accounts ADD COLUMN consented_at TIMESTAMPTZ;
ALTER TABLE channel_accounts ADD COLUMN consent_version VARCHAR(30);
ALTER TABLE channel_link_tokens ADD COLUMN consented_at TIMESTAMPTZ;
ALTER TABLE channel_link_tokens ADD COLUMN consent_version VARCHAR(30);

-- Do not invent consent for links created before explicit consent existed.
-- Invalidate outstanding legacy tokens so the next attempt requires consent.
UPDATE channel_link_tokens SET used_at = CURRENT_TIMESTAMP WHERE used_at IS NULL;

ALTER TABLE channel_accounts DROP CONSTRAINT uk_channel_accounts_channel_linked;
ALTER TABLE channel_accounts DROP CONSTRAINT uk_channel_accounts_channel_external_user;
CREATE UNIQUE INDEX uk_channel_accounts_active_user
    ON channel_accounts(channel_type, user_id) WHERE revoked_at IS NULL;
CREATE UNIQUE INDEX uk_channel_accounts_active_external_user
    ON channel_accounts(channel_type, external_user_id) WHERE revoked_at IS NULL;
CREATE UNIQUE INDEX uk_channel_accounts_active_chat
    ON channel_accounts(channel_type, external_chat_id) WHERE revoked_at IS NULL;
