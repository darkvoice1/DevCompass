CREATE INDEX idx_task_timeline_due_date
    ON task (due_date, id)
    WHERE deleted_at IS NULL
      AND due_date IS NOT NULL
      AND status <> 'CANCELLED';

CREATE INDEX idx_project_timeline_target_date
    ON project (target_date, id)
    WHERE deleted_at IS NULL
      AND archived = FALSE
      AND target_date IS NOT NULL;
