-- Sports Performance Tracker — Database Schema (SQLite)
-- No external database server required; SQLite stores data in a local file.
--
-- The schema is created automatically when the application starts for the first time.
-- You can also run this manually with:
--   sqlite3 sports_tracker.db < database/Schema.sql

CREATE TABLE IF NOT EXISTS users (
    id         INTEGER  PRIMARY KEY AUTOINCREMENT,
    email      TEXT     NOT NULL UNIQUE,
    password   TEXT     NOT NULL,
    role       TEXT     NOT NULL DEFAULT 'athlete',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS performance_records (
    id         INTEGER  PRIMARY KEY AUTOINCREMENT,
    athlete    TEXT     NOT NULL,
    sport      TEXT     NOT NULL DEFAULT 'Running',
    distance   REAL     NOT NULL,
    time_sec   REAL     NOT NULL,
    speed      REAL     NOT NULL,
    accuracy   REAL     NOT NULL,  -- auto-calculated speed efficiency (0-100)
    stamina    REAL     NOT NULL,  -- reserved for future use, stored as 0
    score      REAL     NOT NULL,
    level      TEXT     NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tasks (
    id           INTEGER  PRIMARY KEY AUTOINCREMENT,
    coach        TEXT     NOT NULL,
    athlete      TEXT     NOT NULL,
    title        TEXT     NOT NULL,
    description  TEXT     NOT NULL DEFAULT '',
    due_date     TEXT     NOT NULL DEFAULT '',
    status       TEXT     NOT NULL DEFAULT 'pending',
    snooze_count INTEGER  NOT NULL DEFAULT 0,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Demo user: coach@example.com / safePassword123
-- Password stored as PBKDF2-HMAC-SHA256 (310000 iterations): "<hex_salt>:<hex_hash>"
INSERT OR IGNORE INTO users (email, password, role)
VALUES ('coach@example.com',
        'a7c8b5d2e1f4a3b0c9d8e7f6a5b4c3d2:ffb8a51b21e125e1279827b3100256f8ce1b3f8bdd4b616403515f9259aa14a5',
        'coach');

-- Sample records for dashboard demo
INSERT OR IGNORE INTO performance_records (id,athlete,sport,distance,time_sec,speed,accuracy,stamina,score,level) VALUES
(1,'coach@example.com','Running',400,65,6.15,78,70,55.86,'Average'),
(2,'coach@example.com','Running',400,62,6.45,80,72,59.38,'Average'),
(3,'coach@example.com','Running',400,58,6.90,83,75,64.26,'Average'),
(4,'coach@example.com','Running',400,55,7.27,85,78,68.31,'Average'),
(5,'coach@example.com','Running',400,52,7.69,87,80,72.87,'Good'),
(6,'coach@example.com','Running',400,50,8.00,88,82,74.86,'Good');

SELECT 'Database setup complete.' AS status;
