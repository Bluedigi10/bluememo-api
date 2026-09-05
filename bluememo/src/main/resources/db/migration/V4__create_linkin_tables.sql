CREATE TABLE channel_link_tokens (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    channel_type VARCHAR(30) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    is_used BOOLEAN,

    CONSTRAINT pk_channel_link_tokens PRIMARY KEY (id),
    CONSTRAINT fk_channel_link_tokens_by_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),
    CONSTRAINT uk_channel_link_tokens_channel_type_user
        UNIQUE (channel_type, user_id)
);

CREATE TABLE channel_accounts (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    channel_type VARCHAR(30) NOT NULL,
    external_user_id VARCHAR(255) NOT NULL,
    external_chat_id VARCHAR(255),
    linked_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_channel_accounts PRIMARY KEY (id),
    CONSTRAINT fk_channel_accounts_by_user
        FOREIGN KEY (user_id)
        REFERENCES users (id),
    CONSTRAINT uk_channel_accounts_channel_linked
        UNIQUE (channel_type, user_id)
);
