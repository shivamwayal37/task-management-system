CREATE TABLE pending_notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    task_title VARCHAR(255) NOT NULL,
    update_type VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP
);

CREATE INDEX idx_pending_notifications_processed
ON pending_notifications(processed);

CREATE INDEX idx_pending_notifications_user_processed
ON pending_notifications(user_id, processed);
