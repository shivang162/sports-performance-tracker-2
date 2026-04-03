package com.tracker.dao;

import java.sql.*;
import java.util.*;

public class PerformanceDAO {

    /** Insert a new session record into PostgreSQL */
    public boolean insertRecord(String athlete, String sport, double distance, double timeSec,
                                double speed, double accuracy, double stamina,
                                double score, String level) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO performance_records " +
                 "(athlete,sport,distance,time_sec,speed,accuracy,stamina,score,level) " +
                 "VALUES (?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, athlete);
            ps.setString(2, sport);
            ps.setDouble(3, distance); ps.setDouble(4, timeSec);
            ps.setDouble(5, speed);    ps.setDouble(6, accuracy);
            ps.setDouble(7, stamina);  ps.setDouble(8, score);
            ps.setString(9, level);
            int rows = ps.executeUpdate();
            System.out.println("[PerformanceDAO] Inserted for: " + athlete + " sport=" + sport);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[PerformanceDAO] insertRecord: " + e.getMessage());
            return false;
        }
    }

    /** Scores for a specific athlete + sport ordered by time — used by PerformanceService for trend/avg */
    public List<Double> getAllScores(String athlete, String sport) {
        List<Double> scores = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT score FROM performance_records WHERE athlete=? AND sport=? ORDER BY created_at ASC")) {
            ps.setString(1, athlete);
            ps.setString(2, sport);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) scores.add(rs.getDouble("score"));
            }
        } catch (SQLException e) {
            System.err.println("[PerformanceDAO] getAllScores: " + e.getMessage());
        }
        return scores;
    }

    /** All records WITH athlete name and sport — used by /records endpoint */
    public List<Object[]> getAllRecordsWithAthlete() {
        List<Object[]> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT athlete,sport,distance,time_sec,speed,accuracy,stamina,score,level " +
                 "FROM performance_records ORDER BY created_at ASC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{
                        rs.getString("athlete"),
                        rs.getString("sport"),
                        rs.getDouble("distance"), rs.getDouble("time_sec"),
                        rs.getDouble("speed"),    rs.getDouble("accuracy"),
                        rs.getDouble("stamina"),  rs.getDouble("score"),
                        rs.getString("level")
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("[PerformanceDAO] getAllRecordsWithAthlete: " + e.getMessage());
        }
        return list;
    }

    /** Records for a specific athlete only — used by /records?athlete=email for athlete role */
    public List<Object[]> getRecordsByAthlete(String athlete) {
        List<Object[]> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT athlete,sport,distance,time_sec,speed,accuracy,stamina,score,level " +
                 "FROM performance_records WHERE athlete=? ORDER BY created_at ASC")) {
            ps.setString(1, athlete);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{
                        rs.getString("athlete"),
                        rs.getString("sport"),
                        rs.getDouble("distance"), rs.getDouble("time_sec"),
                        rs.getDouble("speed"),    rs.getDouble("accuracy"),
                        rs.getDouble("stamina"),  rs.getDouble("score"),
                        rs.getString("level")
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("[PerformanceDAO] getRecordsByAthlete: " + e.getMessage());
        }
        return list;
    }

    /** Records for a specific athlete + sport — used by /records?athlete=email&sport=X */
    public List<Object[]> getRecordsByAthleteSport(String athlete, String sport) {
        List<Object[]> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT athlete,sport,distance,time_sec,speed,accuracy,stamina,score,level " +
                 "FROM performance_records WHERE athlete=? AND sport=? ORDER BY created_at ASC")) {
            ps.setString(1, athlete);
            ps.setString(2, sport);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{
                        rs.getString("athlete"),
                        rs.getString("sport"),
                        rs.getDouble("distance"), rs.getDouble("time_sec"),
                        rs.getDouble("speed"),    rs.getDouble("accuracy"),
                        rs.getDouble("stamina"),  rs.getDouble("score"),
                        rs.getString("level")
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("[PerformanceDAO] getRecordsByAthleteSport: " + e.getMessage());
        }
        return list;
    }

    /** Session count for a specific athlete + sport — shown on dashboard */
    public int getTotalCount(String athlete, String sport) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) AS cnt FROM performance_records WHERE athlete=? AND sport=?")) {
            ps.setString(1, athlete);
            ps.setString(2, sport);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            System.err.println("[PerformanceDAO] getTotalCount: " + e.getMessage());
        }
        return 0;
    }
}
