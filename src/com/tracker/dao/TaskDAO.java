package com.tracker.dao;

import java.sql.*;
import java.util.*;

public class TaskDAO {

    /** Insert a new task created by a coach for an athlete */
    public int insertTask(String coach, String athlete, String title, String description, String dueDate) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO tasks (coach, athlete, title, description, due_date) VALUES (?,?,?,?,?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, coach);
            ps.setString(2, athlete);
            ps.setString(3, title);
            ps.setString(4, description);
            ps.setString(5, dueDate);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[TaskDAO] insertTask: " + e.getMessage());
        }
        return -1;
    }

    /** Get all tasks created by a specific coach */
    public List<Map<String, Object>> getTasksByCoach(String coach) {
        return queryTasks("SELECT * FROM tasks WHERE coach=? ORDER BY created_at DESC", coach);
    }

    /** Get all tasks assigned to a specific athlete */
    public List<Map<String, Object>> getTasksByAthlete(String athlete) {
        return queryTasks("SELECT * FROM tasks WHERE athlete=? ORDER BY created_at DESC", athlete);
    }

    /**
     * Update a task's status.
     * If action is "snoozed", increments snooze_count; if snooze_count reaches 3,
     * the status is automatically set to "missed" instead.
     * Returns the final status applied.
     */
    public String updateTaskStatus(int taskId, String action) {
        try (Connection conn = DBConnection.getConnection()) {
            // Fetch current snooze_count
            int snoozeCount = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT snooze_count FROM tasks WHERE id=?")) {
                ps.setInt(1, taskId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) snoozeCount = rs.getInt("snooze_count");
                    else return null;
                }
            }

            String finalStatus;
            int newSnoozeCount = snoozeCount;

            if ("snoozed".equals(action)) {
                newSnoozeCount = snoozeCount + 1;
                finalStatus = newSnoozeCount >= 3 ? "missed" : "snoozed";
            } else {
                finalStatus = action; // "completed" or "missed"
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE tasks SET status=?, snooze_count=? WHERE id=?")) {
                ps.setString(1, finalStatus);
                ps.setInt(2, newSnoozeCount);
                ps.setInt(3, taskId);
                ps.executeUpdate();
            }
            return finalStatus;
        } catch (SQLException e) {
            System.err.println("[TaskDAO] updateTaskStatus: " + e.getMessage());
            return null;
        }
    }

    /** Returns the current snooze_count for a task (0 if not found). */
    public int getSnoozeCount(int taskId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT snooze_count FROM tasks WHERE id=?")) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("snooze_count");
            }
        } catch (SQLException e) {
            System.err.println("[TaskDAO] getSnoozeCount: " + e.getMessage());
        }
        return 0;
    }

    private List<Map<String, Object>> queryTasks(String sql, String param) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id",          rs.getInt("id"));
                    row.put("coach",       rs.getString("coach"));
                    row.put("athlete",     rs.getString("athlete"));
                    row.put("title",       rs.getString("title"));
                    row.put("description", rs.getString("description"));
                    row.put("dueDate",     rs.getString("due_date"));
                    row.put("status",      rs.getString("status"));
                    row.put("snoozeCount", rs.getInt("snooze_count"));
                    row.put("createdAt",   rs.getString("created_at"));
                    list.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("[TaskDAO] queryTasks: " + e.getMessage());
        }
        return list;
    }
}
