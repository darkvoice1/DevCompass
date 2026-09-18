CREATE INDEX idx_project_active_unarchived_status
    ON project (status, updated_at DESC)
    WHERE deleted_at IS NULL AND archived = FALSE;

CREATE INDEX idx_task_active_project_updated
    ON task (project_id, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_work_log_active_task_updated
    ON work_log (task_id, updated_at DESC)
    WHERE deleted_at IS NULL;
