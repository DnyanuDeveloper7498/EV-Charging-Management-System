# EV Charging API (Spring Boot 3.5 / Java 21)

## Run locally
1. Create a PostgreSQL database `ev_charging_system` (empty is fine - Flyway creates all tables).
2. Set environment variables (see `.env.example`): `DB_PASSWORD`, `JWT_SECRET` (>= 32 chars), optionally
   `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`, `ADMIN_EMAILS`, `CORS_ALLOWED_ORIGINS`.
3. `mvn clean verify` then `mvn spring-boot:run`.

* Existing database (created from the old `ev_charging_system.sql`): just start the app. Flyway baselines it at V1 and applies `V2__security_wallet_payments.sql`.
* First admin: register normally, then set `ADMIN_EMAILS=you@example.com` and restart (or `UPDATE users SET role='ADMIN' WHERE email='you@example.com';`).
* Local wallet testing without Razorpay: `WALLET_DEMO_TOPUP=true` (never in production).
* Razorpay webhook: `POST https://<api>/api/payments/webhook`, events `payment.captured`, `order.paid`, `payment.failed`, secret = `RAZORPAY_WEBHOOK_SECRET`.
