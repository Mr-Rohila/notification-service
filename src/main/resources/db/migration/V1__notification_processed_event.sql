CREATE TABLE notification_processed_event (
    id                 VARCHAR(36)  NOT NULL,
    event_id           VARCHAR(36)  NOT NULL,
    business_key       CHAR(64)     NOT NULL,
    event_type         VARCHAR(64)  NOT NULL,
    event_version      INT          NOT NULL,
    user_id            VARCHAR(36)  NULL,
    source             VARCHAR(64)  NULL,
    service_request_id VARCHAR(64)  NULL,
    status             VARCHAR(16)  NOT NULL,
    created_at         TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at         TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_npe_event_id (event_id),
    UNIQUE KEY uk_npe_business_key (business_key),
    KEY ix_npe_created_at (created_at),
    KEY ix_npe_status (status)
);
