CREATE TABLE incoming_events (
    id UUID NOT NULL,
    channel_type VARCHAR(30) NOT NULL,
    external_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255),
    status VARCHAR(30),
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_incoming_events PRIMARY KEY (id),
    CONSTRAINT uk_incoming_events_channel_update
        UNIQUE (channel_type, external_event_id)
);

CREATE INDEX ix_incoming_events_received_at
    ON incoming_events (received_at);