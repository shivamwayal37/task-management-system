ALTER TABLE tasks
ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by BIGINT;

CREATE INDEX idx_tasks_deleted ON tasks(deleted);
