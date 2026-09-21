CREATE INDEX idx_activity_project_created
    ON activity (project_id, created_at DESC, id DESC);

CREATE INDEX idx_activity_project_object_type_created
    ON activity (project_id, object_type, created_at DESC, id DESC);
