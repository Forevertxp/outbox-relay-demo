CREATE TABLE outbox_event (
    event_id       VARCHAR(64)  PRIMARY KEY,
    aggregate_type VARCHAR(64)  NOT NULL,
    aggregate_id   VARCHAR(64)  NOT NULL,
    event_type     VARCHAR(128) NOT NULL,
    payload        JSON         NOT NULL,
    status         VARCHAR(16)  NOT NULL,
    attempts       INT          NOT NULL DEFAULT 0,
    available_at   TIMESTAMP    NOT NULL,
    locked_until   TIMESTAMP    NULL,
    last_error     VARCHAR(1000) NULL,
    created_at     TIMESTAMP    NOT NULL,
    sent_at        TIMESTAMP    NULL
);

CREATE INDEX ix_outbox_ready ON outbox_event(status, available_at);

CREATE TABLE processed_event (
    consumer_name VARCHAR(80) NOT NULL,
    event_id      VARCHAR(64) NOT NULL,
    processed_at  TIMESTAMP NOT NULL,
    PRIMARY KEY (consumer_name, event_id)
);

