ALTER TABLE work_log
    ADD COLUMN commit_hashes VARCHAR(500);

ALTER TABLE work_log
    ADD CONSTRAINT work_log_commit_hashes_format_check
        CHECK (commit_hashes IS NULL OR commit_hashes ~* '^[0-9a-f]{7,12}([[:space:]]*,[[:space:]]*[0-9a-f]{7,12})*$');
