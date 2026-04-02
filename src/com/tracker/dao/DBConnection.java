package com.tracker.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton MySQL connection manager.
 * Change DB_PASS to match your MySQL password.
 */
public class DBConnection {

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/sports_tracker";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "SHIVANG123@k";   // ← your password

    private static Connection connection = null;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                System.out.println("[DBConnection] Connected to MySQL.");
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL driver not found. Add mysql-connector-java JAR to lib/", e);
            }
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try { connection.close(); connection = null; }
            catch (SQLException e) { System.err.println("[DBConnection] Close error: " + e.getMessage()); }
        }
    }
}