-- Smart EV Charging System - PostgreSQL schema
-- Create the database separately: CREATE DATABASE ev_charging_system;

CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    mobile_no VARCHAR(15) NOT NULL,
    password VARCHAR(500) NOT NULL,
    wallet_balance NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (wallet_balance >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id SERIAL PRIMARY KEY,
    vehicle_number VARCHAR(30) NOT NULL UNIQUE,
    brand VARCHAR(80) NOT NULL,
    model VARCHAR(80) NOT NULL,
    vehicle_type VARCHAR(40) NOT NULL,
    battery_percentage NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK (battery_percentage BETWEEN 0 AND 100),
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    battery_capacity NUMERIC(8,2) NOT NULL CHECK (battery_capacity > 0)
);

CREATE TABLE IF NOT EXISTS charging_stations (
    station_id SERIAL PRIMARY KEY,
    station_name VARCHAR(150) NOT NULL,
    s_location VARCHAR(250) NOT NULL,
    total_slots INT NOT NULL CHECK (total_slots > 0),
    available_slots INT NOT NULL CHECK (available_slots BETWEEN 0 AND total_slots),
    price_per_kwh NUMERIC(10,2) NOT NULL CHECK (price_per_kwh >= 0),
    charger_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE'
);

CREATE TABLE IF NOT EXISTS bookings (
    booking_id SERIAL PRIMARY KEY,
    vehicle_id INT NOT NULL REFERENCES vehicles(vehicle_id) ON DELETE CASCADE,
    station_id INT NOT NULL REFERENCES charging_stations(station_id) ON DELETE RESTRICT,
    booking_time TIMESTAMP NOT NULL,
    booking_status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED'
);

CREATE TABLE IF NOT EXISTS charging_sessions (
    session_id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL UNIQUE REFERENCES bookings(booking_id) ON DELETE CASCADE,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    units_consumed NUMERIC(10,3) NOT NULL DEFAULT 0,
    total_cost NUMERIC(12,2) NOT NULL DEFAULT 0,
    start_battery_percentage NUMERIC(5,2) NOT NULL CHECK (start_battery_percentage BETWEEN 0 AND 100),
    end_battery_percentage NUMERIC(5,2) NULL CHECK (end_battery_percentage BETWEEN 0 AND 100)
);

CREATE TABLE IF NOT EXISTS payments (
    payment_id SERIAL PRIMARY KEY,
    session_id INT NOT NULL REFERENCES charging_sessions(session_id) ON DELETE CASCADE,
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    payment_method VARCHAR(40) NOT NULL,
    payment_status VARCHAR(30) NOT NULL,
    payment_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vehicles_user_id ON vehicles(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_vehicle_id ON bookings(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_bookings_station_time ON bookings(station_id, booking_time);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(booking_status);
CREATE INDEX IF NOT EXISTS idx_sessions_booking_id ON charging_sessions(booking_id);
CREATE INDEX IF NOT EXISTS idx_payments_session_id ON payments(session_id);

INSERT INTO charging_stations
    (station_name, s_location, total_slots, available_slots, price_per_kwh, charger_type, status)
SELECT 'Pune EV Hub', 'Pune, Maharashtra', 6, 6, 12.50, 'DC', 'AVAILABLE'
WHERE NOT EXISTS (SELECT 1 FROM charging_stations);

INSERT INTO charging_stations
    (station_name, s_location, total_slots, available_slots, price_per_kwh, charger_type, status)
SELECT 'City Charge Point', 'Pune City', 4, 4, 10.00, 'AC', 'AVAILABLE'
WHERE (SELECT COUNT(*) FROM charging_stations) = 1;
