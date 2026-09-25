ALTER TABLE notification_delivery ADD COLUMN callback_status VARCHAR(16) NULL;
ALTER TABLE notification_delivery ADD COLUMN callback_attempts INT NOT NULL DEFAULT 0;

CREATE INDEX idx_nd_callback ON notification_delivery (callback_status, created_at);
