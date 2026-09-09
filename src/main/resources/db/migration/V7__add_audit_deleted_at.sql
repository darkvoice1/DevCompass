ALTER TABLE project
    ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE project_phase
    ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE task
    ADD COLUMN deleted_at TIMESTAMPTZ;
