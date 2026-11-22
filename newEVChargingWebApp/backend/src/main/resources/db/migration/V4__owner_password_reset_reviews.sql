-- V4: station ownership, password reset tokens, station reviews.
-- Idempotent, like V2.

-- ------------------------------------------------------- station owner
ALTER TABLE charging_stations ADD COLUMN IF NOT EXISTS owner_id INT REFERENCES users(user_id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_stations_owner ON charging_stations (owner_id);

-- ------------------------------------------------- password reset tokens
-- Only a SHA-256 hash of the token is stored; the raw token exists only in the e-mailed link.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    token_id    SERIAL PRIMARY KEY,
    user_id     INT          NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_prt_hash ON password_reset_tokens (token_hash);
CREATE INDEX        IF NOT EXISTS idx_prt_user ON password_reset_tokens (user_id);

-- ---------------------------------------------------- station reviews
CREATE TABLE IF NOT EXISTS station_reviews (
    review_id   SERIAL PRIMARY KEY,
    station_id  INT          NOT NULL REFERENCES charging_stations(station_id) ON DELETE CASCADE,
    user_id     INT          NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    rating      INT          NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- one review per user per station (posting again updates it)
CREATE UNIQUE INDEX IF NOT EXISTS ux_review_station_user ON station_reviews (station_id, user_id);
CREATE INDEX        IF NOT EXISTS idx_review_station      ON station_reviews (station_id);
