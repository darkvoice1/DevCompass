CREATE TABLE attachment (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project(id),
    original_file_name VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_key VARCHAR(36) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT attachment_size_bytes_check CHECK (size_bytes > 0),
    CONSTRAINT attachment_storage_key_unique UNIQUE (storage_key)
);

CREATE INDEX idx_attachment_active_project_created
    ON attachment (project_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;
