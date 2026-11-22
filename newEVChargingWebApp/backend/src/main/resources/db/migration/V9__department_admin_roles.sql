ALTER TABLE users
DROP CONSTRAINT IF EXISTS chk_users_role;

ALTER TABLE users
ADD CONSTRAINT chk_users_role
CHECK (
    role IN (
        'USER',
        'ADMIN',
        'STATION_OWNER',
        'USER_ADMIN',
        'VEHICLE_ADMIN',
        'CHARGING_ADMIN',
        'FINANCE_ADMIN',
        'SUPPORT_ADMIN',
        'STATION_ADMIN'
    )
);