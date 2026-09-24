CREATE TABLE refresh_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES user_account(id),
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_token_id BIGINT REFERENCES refresh_token(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT refresh_token_hash_unique UNIQUE (token_hash),
    CONSTRAINT refresh_token_hash_length_check CHECK (CHAR_LENGTH(token_hash) = 64),
    CONSTRAINT refresh_token_expiration_check CHECK (expires_at > created_at)
);

CREATE INDEX idx_refresh_token_active_user
    ON refresh_token (user_id, expires_at DESC)
    WHERE revoked_at IS NULL;
