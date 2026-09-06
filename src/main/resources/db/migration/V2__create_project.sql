CREATE TABLE project (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    target_date DATE,
    tech_stack VARCHAR(500),
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT project_status_check CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'PAUSED'))
);

CREATE INDEX idx_project_archived_status ON project (archived, status);
