CREATE INDEX idx_project_active_unarchived_updated
    ON project (updated_at DESC, id DESC)
    WHERE deleted_at IS NULL AND archived = FALSE;

CREATE INDEX idx_task_active_updated
    ON task (updated_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_work_log_active_updated
    ON work_log (updated_at DESC, id DESC)
    WHERE deleted_at IS NULL;
