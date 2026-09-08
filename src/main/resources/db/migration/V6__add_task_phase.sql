ALTER TABLE task
    ADD COLUMN phase_id BIGINT NOT NULL REFERENCES project_phase(id);

CREATE INDEX idx_task_project_phase ON task (project_id, phase_id);
