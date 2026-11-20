CREATE TABLE lanes (
    id BIGINT PRIMARY KEY,
    origin VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transporters (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lane_quotes (
    id BIGSERIAL PRIMARY KEY,
    lane_id BIGINT NOT NULL,
    transporter_id BIGINT NOT NULL,
    quote NUMERIC(19, 2) NOT NULL CHECK (quote > 0),
    CONSTRAINT fk_lane_quote_lane FOREIGN KEY (lane_id) REFERENCES lanes(id) ON DELETE CASCADE,
    CONSTRAINT fk_lane_quote_transporter FOREIGN KEY (transporter_id) REFERENCES transporters(id) ON DELETE CASCADE,
    CONSTRAINT uk_lane_quote_lane_transporter UNIQUE (lane_id, transporter_id)
);

CREATE INDEX idx_lane_quote_lane_id ON lane_quotes(lane_id);
CREATE INDEX idx_lane_quote_transporter_id ON lane_quotes(transporter_id);
