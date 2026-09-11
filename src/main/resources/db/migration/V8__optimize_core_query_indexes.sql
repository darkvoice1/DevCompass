CREATE INDEX idx_project_active_status
    ON project (status, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_project_phase_active_project_sort
    ON project_phase (project_id, sort_order, id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_task_active_project_status_priority_due
    ON task (project_id, status, priority, due_date, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_task_active_project_phase_due
    ON task (project_id, phase_id, due_date, updated_at DESC)
    WHERE deleted_at IS NULL;
