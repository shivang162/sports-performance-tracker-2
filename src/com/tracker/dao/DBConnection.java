package com.tracker.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * PostgreSQL connection factory.
 * Credentials are read from environment variables DB_URL, DB_USER, and DB_PASS.
 * If the environment variables are not set, the values below are used as defaults.
 *
 * Set before running:
 *   export DB_URL=jdbc:postgresql://localhost:5432/sports_tracker
 *   export DB_USER=postgres
 *   export DB_PASS=your_password
 */
public class DBConnection {

    private static final String DB_URL  = System.getenv("DB_URL")  != null
            ? System.getenv("DB_URL")  : "jdbc:postgresql://localhost:5432/sports_tracker";
    private static final String DB_USER = System.getenv("DB_USER") != null
            ? System.getenv("DB_USER") : "postgres";
    private static final String DB_PASS = System.getenv("DB_PASS") != null
            ? System.getenv("DB_PASS") : "";

    // Not a singleton — a new connection is created per call so that concurrent
    // request threads each get their own Connection (java.sql.Connection is not
    // thread-safe and must not be shared across threads).
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL driver not found. Add postgresql JDBC JAR to lib/", e);
        }
    }

    /** No-op kept for API compatibility; connections are now closed by callers. */
    public static void closeConnection() {}
}