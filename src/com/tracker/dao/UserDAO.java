package com.tracker.dao;

import java.sql.*;

public class UserDAO {

    /** Validate login — queries MySQL, falls back to hardcoded demo if DB is down */
    public boolean validateUser(String email, String password) {
        if (email == null || password == null) return false;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id FROM users WHERE email=? AND password=? LIMIT 1")) {
            ps.setString(1, email.trim());
            ps.setString(2, password.trim());
            try (ResultSet rs = ps.executeQuery()) {
                boolean found = rs.next();
                System.out.println("[UserDAO] Login " + email + ": " + (found ? "OK" : "FAIL"));
                return found;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] DB unavailable, fallback: " + e.getMessage());
            return email.equals("coach@example.com") && password.equals("safePassword123");
        }
    }

    /** Register new user — returns false if email already exists */
    public boolean insertUser(String email, String password, String role) {
        if (email == null || password == null) return false;
        if (emailExists(email)) return false;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO users (email,password,role) VALUES (?,?,?)")) {
            ps.setString(1, email.trim());
            ps.setString(2, password.trim());
            ps.setString(3, role != null ? role : "athlete");
            int rows = ps.executeUpdate();
            System.out.println("[UserDAO] Registered: " + email);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[UserDAO] insertUser failed: " + e.getMessage());
            return false;
        }
    }

    public boolean emailExists(String email) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id FROM users WHERE email=? LIMIT 1")) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) { return false; }
    }
}