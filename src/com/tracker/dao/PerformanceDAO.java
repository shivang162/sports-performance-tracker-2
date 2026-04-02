package com.tracker.dao;

import java.sql.*;
import java.util.*;

public class PerformanceDAO {

    /** Insert a new session record into MySQL */
    public boolean insertRecord(String athlete, double distance, double timeSec,
                                double speed, double accuracy, double stamina,
                                double score, String level) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO performance_records " +
                 "(athlete,distance,time_sec,speed,accuracy,stamina,score,level) " +
                 "VALUES (?,?,?,?,?,?,?,?)")) {
            ps.setString(1, athlete);
            ps.setDouble(2, distance); ps.setDouble(3, timeSec);
            ps.setDouble(4, speed);    ps.setDouble(5, accuracy);
            ps.setDouble(6, stamina);  ps.setDouble(7, score);
            ps.setString(8, level);
            int rows = ps.executeUpdate();
            System.out.println("[PerformanceDAO] Inserted for: " + athlete);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[PerformanceDAO] insertRecord: " + e.getMessage());
            return false;
        }
    }

    /** All scores ordered by time — used by PerformanceService for trend/avg */
    public List<Double> getAllScores() {
        List<Double> scores = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT score FROM performance_records ORDER BY created_at ASC")) {
            while (rs.next()) scores.add(rs.getDouble("score"));
        } catch (SQLException e) {
            System.err.println("[PerformanceDAO] getAllScores: " + e.getMessage());
        }
        return scores;
    }

    /** All records WITH athlete name — used by /records endpoint */
    public List<Object[]> getAllRecordsWithAthlete() {
        List<Object[]> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT athlete,distance,time_sec,speed,accuracy,stamina,score,level " +
                 "FROM performance_records ORDER BY created_at ASC")) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getString("athlete"),
                    rs.getDouble("distance"), rs.getDouble("time_sec"),
                    rs.getDouble("speed"),    rs.getDouble("accuracy"),
                    rs.getDouble("stamina"),  rs.getDouble("score"),
                    rs.getString("level")
                });
            }
        } catch (SQLException e) {
            System.err.println("[PerformanceDAO] getAllRecordsWithAthlete: " + e.getMessage());
        }
        return list;
    }

    /** Total session count — shown on dashboard */
    public int getTotalCount() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM performance_records")) {
            if (rs.next()) return rs.getInt("cnt");
        } catch (SQLException e) {
            System.err.println("[PerformanceDAO] getTotalCount: " + e.getMessage());
        }
        return 0;
    }
}