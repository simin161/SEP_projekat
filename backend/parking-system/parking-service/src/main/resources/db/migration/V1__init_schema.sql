CREATE TABLE app_user (
    id                UUID PRIMARY KEY,
    email             VARCHAR(255) NOT NULL UNIQUE,
    password_hash     VARCHAR(255) NOT NULL,
    full_name         VARCHAR(255) NOT NULL,
    role              VARCHAR(255) NOT NULL CHECK (role IN ('DRIVER')),
    notify_before_min INTEGER      NOT NULL DEFAULT 5,
    created_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE TABLE zone (
    id               UUID PRIMARY KEY,
    code             VARCHAR(255) NOT NULL UNIQUE,
    name             VARCHAR(255) NOT NULL,
    location         VARCHAR(255) NOT NULL,
    currency         CHAR(3)      NOT NULL,
    max_duration_min INTEGER      NOT NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE zone_rule (
    zone_id            UUID PRIMARY KEY REFERENCES zone (id),
    min_duration_min   INTEGER NOT NULL,
    step_min           INTEGER NOT NULL,
    billing_from       TIME(6),
    billing_to         TIME(6),
    extensions_allowed BOOLEAN NOT NULL,
    max_extensions     INTEGER,
    max_extension_min  INTEGER NOT NULL
);

CREATE TABLE tariff (
    id             UUID PRIMARY KEY,
    zone_id        UUID           NOT NULL REFERENCES zone (id),
    time_from      TIME(6)        NOT NULL,
    time_to        TIME(6)        NOT NULL,
    price_per_hour NUMERIC(12, 2) NOT NULL
);

CREATE INDEX idx_tariff_zone ON tariff (zone_id);

CREATE TABLE vehicle (
    id           UUID PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES app_user (id),
    plate_number VARCHAR(255) NOT NULL,
    label        VARCHAR(255),
    CONSTRAINT uq_vehicle_user_plate UNIQUE (user_id, plate_number)
);

CREATE TABLE parking_session (
    id               UUID PRIMARY KEY,
    user_id          UUID         NOT NULL REFERENCES app_user (id),
    vehicle_id       UUID         NOT NULL REFERENCES vehicle (id),
    zone_id          UUID         NOT NULL REFERENCES zone (id),
    status           VARCHAR(255) NOT NULL
        CHECK (status IN ('ACTIVE', 'EXPIRED_PENDING_EXTENSION', 'EXPIRED')),
    start_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    end_at           TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    extensions_count INTEGER      NOT NULL DEFAULT 0,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_parking_session_user ON parking_session (user_id);
CREATE INDEX idx_parking_session_status_end ON parking_session (status, end_at);

CREATE TABLE parking_order (
    id                    UUID PRIMARY KEY,
    user_id               UUID           NOT NULL REFERENCES app_user (id),
    vehicle_id            UUID           NOT NULL REFERENCES vehicle (id),
    zone_id               UUID           NOT NULL REFERENCES zone (id),
    session_id            UUID REFERENCES parking_session (id),
    type                  VARCHAR(255)   NOT NULL CHECK (type IN ('INITIAL', 'EXTENSION')),
    status                VARCHAR(255)   NOT NULL
        CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'COMPLETED', 'FAILED', 'EXPIRED',
                          'REFUND_REQUIRED', 'REFUNDED')),
    duration_min          INTEGER        NOT NULL,
    amount                NUMERIC(12, 2) NOT NULL,
    currency              CHAR(3)        NOT NULL,
    merchant_order_id     VARCHAR(255)   NOT NULL UNIQUE,
    psp_payment_id        VARCHAR(255),
    global_transaction_id VARCHAR(255),
    expires_at            TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_at            TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_parking_order_user ON parking_order (user_id);
CREATE INDEX idx_parking_order_session ON parking_order (session_id);
CREATE INDEX idx_parking_order_psp_payment ON parking_order (psp_payment_id);
CREATE INDEX idx_parking_order_status_expires ON parking_order (status, expires_at);

CREATE TABLE extension (
    id              UUID PRIMARY KEY,
    session_id      UUID         NOT NULL REFERENCES parking_session (id),
    order_id        UUID         NOT NULL UNIQUE REFERENCES parking_order (id),
    duration_min    INTEGER      NOT NULL,
    previous_end_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    initiated_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    status          VARCHAR(255) NOT NULL CHECK (status IN ('PENDING', 'APPLIED', 'REJECTED')),
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX uq_extension_session_pending ON extension (session_id) WHERE status = 'PENDING';

CREATE TABLE scheduled_notification (
    id         UUID PRIMARY KEY,
    session_id UUID         NOT NULL UNIQUE REFERENCES parking_session (id),
    send_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    status     VARCHAR(255) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts   INTEGER      NOT NULL DEFAULT 0,
    sent_at    TIMESTAMP(6) WITH TIME ZONE
);

CREATE INDEX idx_scheduled_notification_due ON scheduled_notification (send_at) WHERE status = 'PENDING';

CREATE TABLE status_change (
    id          BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    entity_type VARCHAR(255) NOT NULL CHECK (entity_type IN ('ORDER', 'SESSION', 'EXTENSION')),
    entity_id   UUID         NOT NULL,
    from_status VARCHAR(255),
    to_status   VARCHAR(255) NOT NULL,
    reason      VARCHAR(255),
    changed_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_status_change_entity ON status_change (entity_type, entity_id);

CREATE TABLE webhook_inbox (
    event_id       VARCHAR(255) PRIMARY KEY,
    type           VARCHAR(255) NOT NULL,
    psp_payment_id VARCHAR(255) NOT NULL,
    received_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    processed_at   TIMESTAMP(6) WITH TIME ZONE
);
