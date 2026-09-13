ALTER TABLE project
    ADD COLUMN progress_mode VARCHAR(20) NOT NULL DEFAULT 'AUTO',
    ADD COLUMN auto_progress INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN manual_progress INTEGER,
    ADD COLUMN progress_reason VARCHAR(500);

ALTER TABLE project
    ADD CONSTRAINT project_progress_mode_check
        CHECK (progress_mode IN ('AUTO', 'MANUAL'));

ALTER TABLE project
    ADD CONSTRAINT project_auto_progress_check
        CHECK (auto_progress BETWEEN 0 AND 100);

ALTER TABLE project
    ADD CONSTRAINT project_manual_progress_check
        CHECK (manual_progress IS NULL OR manual_progress BETWEEN 0 AND 100);
