CREATE TABLE user_account (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_account_username_trimmed_check CHECK (username = BTRIM(username)),
    CONSTRAINT user_account_username_not_blank_check CHECK (username <> ''),
    CONSTRAINT user_account_password_hash_not_blank_check CHECK (password_hash <> '')
);

CREATE UNIQUE INDEX uk_user_account_username_lower
    ON user_account (LOWER(username));
