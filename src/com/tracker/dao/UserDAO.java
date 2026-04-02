package com.tracker.dao;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.sql.*;
import java.util.HexFormat;

public class UserDAO {

    private static final int ITERATIONS = 310_000;
    private static final int KEY_LEN    = 256;   // bits

    /**
     * Hash a password with PBKDF2-HMAC-SHA256 and a random salt.
     * Returns the stored value as "hexSalt:hexHash" (97 chars total).
     */
    private static String hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            return hexSalt(salt) + ":" + pbkdf2(password, salt);
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    /**
     * Verify a plaintext password against a stored "hexSalt:hexHash" value.
     */
    private static boolean verifyPassword(String password, String stored) {
        try {
            int sep = stored.indexOf(':');
            if (sep == -1) return false;
            byte[] salt     = HexFormat.of().parseHex(stored.substring(0, sep));
            String expected = stored.substring(sep + 1);
            return pbkdf2(password, salt).equals(expected);
        } catch (Exception e) {
            return false;
        }
    }

    private static String pbkdf2(String password, byte[] salt) throws Exception {
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LEN);
        return HexFormat.of().formatHex(skf.generateSecret(spec).getEncoded());
    }

    private static String hexSalt(byte[] salt) {
        return HexFormat.of().formatHex(salt);
    }

    /** Validate login — queries MySQL, verifies PBKDF2 hashed password. */
    public boolean validateUser(String email, String password) {
        if (email == null || password == null) return false;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT password FROM users WHERE email=? LIMIT 1")) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("[UserDAO] Login " + email + ": FAIL (not found)");
                    return false;
                }
                boolean ok = verifyPassword(password.trim(), rs.getString("password"));
                System.out.println("[UserDAO] Login " + email + ": " + (ok ? "OK" : "FAIL"));
                return ok;
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO] DB unavailable: " + e.getMessage());
            return false;
        }
    }

    /** Register new user — stores PBKDF2 hashed password. Returns false if email already exists. */
    public boolean insertUser(String email, String password, String role) {
        if (email == null || password == null) return false;
        if (emailExists(email)) return false;
        String hashed = hashPassword(password.trim());
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO users (email,password,role) VALUES (?,?,?)")) {
            ps.setString(1, email.trim());
            ps.setString(2, hashed);
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
