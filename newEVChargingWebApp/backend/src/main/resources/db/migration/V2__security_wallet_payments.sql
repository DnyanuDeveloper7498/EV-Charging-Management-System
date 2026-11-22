-- V2: roles, wallet transactions, payment hardening.
-- Every statement is idempotent so it is safe on databases that were created
-- from the old ev_charging_system.sql script (Flyway baselines those at V1).

-- ---------------------------------------------------------------- users
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

DO $$
BEGIN
    ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN', 'STATION_OWNER'));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- e-mail must be unique regardless of letter case
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email_lower ON users (LOWER(email));

-- -------------------------------------------------------------- bookings
CREATE INDEX IF NOT EXISTS idx_bookings_vehicle_time ON bookings (vehicle_id, booking_time);

-- --------------------------------------------------------------- payments
-- These columns were mapped by the Payment entity but missing from the old script.
ALTER TABLE payments ADD COLUMN IF NOT EXISTS razorpay_order_id   VARCHAR(100);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS razorpay_payment_id VARCHAR(100);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS failure_reason      VARCHAR(255);

-- one payment record per charging session (business rule)
CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_session            ON payments (session_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_razorpay_order     ON payments (razorpay_order_id)   WHERE razorpay_order_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_razorpay_payment   ON payments (razorpay_payment_id) WHERE razorpay_payment_id IS NOT NULL;
CREATE INDEX        IF NOT EXISTS idx_payments_status            ON payments (payment_status);

-- ----------------------------------------------------- wallet_transactions
CREATE TABLE IF NOT EXISTS wallet_transactions (
    txn_id              SERIAL PRIMARY KEY,
    user_id             INT            NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    txn_type            VARCHAR(20)    NOT NULL CHECK (txn_type IN ('CREDIT', 'DEBIT', 'REFUND')),
    amount              NUMERIC(12,2)  NOT NULL CHECK (amount > 0),
    balance_after       NUMERIC(12,2),
    status              VARCHAR(20)    NOT NULL CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    reference_type      VARCHAR(30)    NOT NULL,
    reference_id        INT,
    razorpay_order_id   VARCHAR(100),
    razorpay_payment_id VARCHAR(100),
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX        IF NOT EXISTS idx_wallet_txn_user          ON wallet_transactions (user_id, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS ux_wallet_txn_rp_order       ON wallet_transactions (razorpay_order_id)   WHERE razorpay_order_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_wallet_txn_rp_payment     ON wallet_transactions (razorpay_payment_id) WHERE razorpay_payment_id IS NOT NULL;
-- a session can be paid from the wallet only once
CREATE UNIQUE INDEX IF NOT EXISTS ux_wallet_txn_session_debit  ON wallet_transactions (reference_id)
    WHERE reference_type = 'SESSION_PAYMENT' AND txn_type = 'DEBIT' AND status = 'SUCCESS';
