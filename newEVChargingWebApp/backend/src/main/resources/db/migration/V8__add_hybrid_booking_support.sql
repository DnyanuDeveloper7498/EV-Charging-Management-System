
ALTER TABLE charging_stations
ADD COLUMN booking_mode VARCHAR(20) NOT NULL DEFAULT 'AUTO';

ALTER TABLE charging_stations
ADD CONSTRAINT chk_charging_station_booking_mode
CHECK (booking_mode IN ('AUTO', 'MANUAL'));

ALTER TABLE bookings
ADD COLUMN approval_expires_at TIMESTAMP NULL;

CREATE INDEX idx_bookings_pending_approval_expiry
ON bookings (approval_expires_at)
WHERE booking_status = 'PENDING_APPROVAL';
