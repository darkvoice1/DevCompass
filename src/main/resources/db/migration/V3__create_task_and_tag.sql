CREATE TABLE task (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project(id),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    due_date DATE,
    estimated_hours INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT task_status_check CHECK (status IN ('TODO', 'IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT task_priority_check CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT task_estimated_hours_check CHECK (estimated_hours IS NULL OR estimated_hours >= 0)
);

CREATE INDEX idx_task_project_status ON task (project_id, status);
CREATE INDEX idx_task_project_priority ON task (project_id, priority);
CREATE INDEX idx_task_project_due_date ON task (project_id, due_date);

CREATE TABLE tag (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project(id),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT tag_project_name_unique UNIQUE (project_id, name)
);

CREATE INDEX idx_tag_project ON tag (project_id);

CREATE TABLE task_tag (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tag(id) ON DELETE CASCADE,
    CONSTRAINT task_tag_unique UNIQUE (task_id, tag_id)
);
