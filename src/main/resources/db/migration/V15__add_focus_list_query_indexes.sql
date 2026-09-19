CREATE INDEX idx_task_active_due_date
    ON task (due_date, id)
    WHERE deleted_at IS NULL
      AND due_date IS NOT NULL
      AND status NOT IN ('COMPLETED', 'CANCELLED');

CREATE INDEX idx_task_active_blocked_updated
    ON task (updated_at DESC, id)
    WHERE deleted_at IS NULL
      AND blocked = TRUE
      AND status NOT IN ('COMPLETED', 'CANCELLED');

CREATE INDEX idx_project_active_unarchived_target_date
    ON project (target_date, id)
    WHERE deleted_at IS NULL
      AND archived = FALSE
      AND status <> 'COMPLETED'
      AND target_date IS NOT NULL;
