
CREATE TABLE notifications (
    notification_id BIGSERIAL PRIMARY KEY,

    user_id INTEGER NOT NULL,

    booking_id INTEGER,

    title VARCHAR(150) NOT NULL,

    message VARCHAR(500) NOT NULL,

    notification_type VARCHAR(50) NOT NULL,

    is_read BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_notification_booking
        FOREIGN KEY (booking_id)
        REFERENCES bookings(booking_id)
        ON DELETE SET NULL
);

CREATE INDEX idx_notifications_user_created
ON notifications(user_id, created_at DESC);

CREATE INDEX idx_notifications_user_read
ON notifications(user_id, is_read);
