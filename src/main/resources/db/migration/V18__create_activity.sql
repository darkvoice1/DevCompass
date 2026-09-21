CREATE TABLE activity (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project(id),
    object_type VARCHAR(30) NOT NULL,
    object_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT activity_object_type_check
        CHECK (object_type IN ('PROJECT', 'TASK', 'PHASE')),
    CONSTRAINT activity_action_check
        CHECK (action IN (
            'CREATED',
            'UPDATED',
            'STATUS_CHANGED',
            'ARCHIVED',
            'RESTORED',
            'DELETED'
        ))
);
