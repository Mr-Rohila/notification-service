CREATE TABLE notification_delivery (
    id                 VARCHAR(36)  NOT NULL,
    event_id           VARCHAR(36)  NOT NULL,
    channel            VARCHAR(16)  NOT NULL DEFAULT 'EMAIL',
    template           VARCHAR(64)  NOT NULL,
    recipient_email    VARCHAR(320) NOT NULL,
    subject            VARCHAR(255) NOT NULL,
    status             VARCHAR(16)  NOT NULL,
    attempt_count      INT          NOT NULL DEFAULT 1,
    error_code         VARCHAR(64)  NULL,
    error_message      VARCHAR(512) NULL,
    service_request_id VARCHAR(64)  NULL,
    sent_at            TIMESTAMP(3) NULL,
    created_at         TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY ix_nd_event_id (event_id),
    KEY ix_nd_created_at (created_at),
    CONSTRAINT fk_nd_event FOREIGN KEY (event_id)
        REFERENCES notification_processed_event (event_id)
);
