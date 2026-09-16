CREATE TABLE work_log (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES task(id),
    log_date DATE NOT NULL,
    plan_content TEXT,
    summary_content TEXT,
    spent_minutes INTEGER NOT NULL DEFAULT 0,
    blocker_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT work_log_spent_minutes_check CHECK (spent_minutes >= 0),
    CONSTRAINT work_log_task_unique UNIQUE (task_id)
);

CREATE INDEX idx_work_log_active_log_date
    ON work_log (log_date DESC)
    WHERE deleted_at IS NULL;
