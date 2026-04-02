-- Sports Performance Tracker — Database Schema (PostgreSQL)
-- Run this FIRST before starting the server:
--   psql -U postgres -f database/Schema.sql

-- Create the database (run as superuser if it doesn't exist yet):
--   psql -U postgres -c "CREATE DATABASE sports_tracker;"

\connect sports_tracker

CREATE TABLE IF NOT EXISTS users (
    id         SERIAL       PRIMARY KEY,
    email      VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'athlete',
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS performance_records (
    id         SERIAL       PRIMARY KEY,
    athlete    VARCHAR(100) NOT NULL,
    distance   DOUBLE PRECISION NOT NULL,
    time_sec   DOUBLE PRECISION NOT NULL,
    speed      DOUBLE PRECISION NOT NULL,
    accuracy   DOUBLE PRECISION NOT NULL,
    stamina    DOUBLE PRECISION NOT NULL,
    score      DOUBLE PRECISION NOT NULL,
    level      VARCHAR(30)  NOT NULL,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- Demo user: coach@example.com / safePassword123
-- Password stored as PBKDF2-HMAC-SHA256 (310000 iterations): "<hex_salt>:<hex_hash>"
-- To update: re-run Schema.sql after changing the password in UserDAO.hashPassword()
INSERT INTO users (email, password, role)
VALUES ('coach@example.com',
        'a7c8b5d2e1f4a3b0c9d8e7f6a5b4c3d2:ffb8a51b21e125e1279827b3100256f8ce1b3f8bdd4b616403515f9259aa14a5',
        'coach')
ON CONFLICT (email) DO NOTHING;

-- Sample records for dashboard demo
INSERT INTO performance_records (athlete, distance, time_sec, speed, accuracy, stamina, score, level) VALUES
('coach@example.com', 400, 65, 6.15, 78, 70, 55.86, 'Average'),
('coach@example.com', 400, 62, 6.45, 80, 72, 59.38, 'Average'),
('coach@example.com', 400, 58, 6.90, 83, 75, 64.26, 'Average'),
('coach@example.com', 400, 55, 7.27, 85, 78, 68.31, 'Average'),
('coach@example.com', 400, 52, 7.69, 87, 80, 72.87, 'Good'),
('coach@example.com', 400, 50, 8.00, 88, 82, 74.86, 'Good')
ON CONFLICT DO NOTHING;

SELECT 'Database setup complete.' AS status;
