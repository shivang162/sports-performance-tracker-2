package com.tracker.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite connection factory — no external database server required.
 * The database is stored in a local file (sports_tracker.db by default).
 *
 * Override the file path via environment variable:
 *   export DB_URL=jdbc:sqlite:/path/to/your/sports_tracker.db
 *
 * Schema is created automatically on first run.
 */
public class DBConnection {

    private static final String DB_URL = System.getenv("DB_URL") != null
            ? System.getenv("DB_URL") : "jdbc:sqlite:sports_tracker.db";

    static {
        initSchema();
    }

    // Not a singleton — a new connection is created per call so that concurrent
    // request threads each get their own Connection (java.sql.Connection is not
    // thread-safe and must not be shared across threads).
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(DB_URL);
            // Enable WAL mode for better concurrent read performance
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
            }
            return conn;
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite driver not found. Add sqlite-jdbc JAR to lib/", e);
        }
    }

    /** No-op kept for API compatibility; connections are closed by callers. */
    public static void closeConnection() {}

    /** Create tables and seed demo data if the database is empty. */
    private static void initSchema() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] SQLite driver not found: " + e.getMessage());
            return;
        }
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement()) {

            st.execute("PRAGMA journal_mode=WAL");

            st.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id         INTEGER  PRIMARY KEY AUTOINCREMENT," +
                "  email      TEXT     NOT NULL UNIQUE," +
                "  password   TEXT     NOT NULL," +
                "  role       TEXT     NOT NULL DEFAULT 'athlete'," +
                "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            st.execute(
                "CREATE TABLE IF NOT EXISTS performance_records (" +
                "  id         INTEGER  PRIMARY KEY AUTOINCREMENT," +
                "  athlete    TEXT     NOT NULL," +
                "  sport      TEXT     NOT NULL DEFAULT 'Running'," +
                "  distance   REAL     NOT NULL," +
                "  time_sec   REAL     NOT NULL," +
                "  speed      REAL     NOT NULL," +
                "  accuracy   REAL     NOT NULL," +
                "  stamina    REAL     NOT NULL," +
                "  score      REAL     NOT NULL," +
                "  level      TEXT     NOT NULL," +
                "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // Seed demo coach account (PBKDF2-HMAC-SHA256 hash of "safePassword123")
            st.execute(
                "INSERT OR IGNORE INTO users (email, password, role) VALUES (" +
                "  'coach@example.com'," +
                "  'a7c8b5d2e1f4a3b0c9d8e7f6a5b4c3d2:ffb8a51b21e125e1279827b3100256f8ce1b3f8bdd4b616403515f9259aa14a5'," +
                "  'coach'" +
                ")"
            );

            // Seed sample performance records for the demo coach account
            String seedRecords =
                "INSERT OR IGNORE INTO performance_records (id,athlete,sport,distance,time_sec,speed,accuracy,stamina,score,level) VALUES " +
                "(1,'coach@example.com','Running',400,65,6.15,78,70,55.86,'Average')," +
                "(2,'coach@example.com','Running',400,62,6.45,80,72,59.38,'Average')," +
                "(3,'coach@example.com','Running',400,58,6.90,83,75,64.26,'Average')," +
                "(4,'coach@example.com','Running',400,55,7.27,85,78,68.31,'Average')," +
                "(5,'coach@example.com','Running',400,52,7.69,87,80,72.87,'Good')," +
                "(6,'coach@example.com','Running',400,50,8.00,88,82,74.86,'Good')";
            st.execute(seedRecords);

            System.out.println("[DBConnection] SQLite schema ready: " + DB_URL);
        } catch (Exception e) {
            System.err.println("[DBConnection] Schema init failed: " + e.getMessage());
        }
    }
}