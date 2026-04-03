package com.tracker.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tracker.dao.TaskDAO;
import com.tracker.service.RecordFormatter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Handles /tasks requests:
 *   GET  /tasks?coach=email   → list tasks created by coach
 *   GET  /tasks?athlete=email → list tasks assigned to athlete
 *   POST /tasks               → create a new task (coach → athlete)
 *   POST /tasks/update        → update task status (complete/snooze/miss)
 */
public class TaskController implements HttpHandler {

    private final TaskDAO        dao = new TaskDAO();
    private final RecordFormatter fmt = new RecordFormatter();

    @Override
    public void handle(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { send(ex, 204, ""); return; }

        String path   = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod().toUpperCase();

        try {
            if (method.equals("GET")) {
                handleGet(ex);
            } else if (method.equals("POST") && path.contains("/update")) {
                handleUpdate(ex);
            } else if (method.equals("POST")) {
                handleCreate(ex);
            } else {
                send(ex, 405, fmt.formatError("Method not allowed"));
            }
        } catch (Exception e) {
            System.err.println("[TaskController] " + e.getMessage());
            send(ex, 500, fmt.formatError("Server error"));
        }
    }

    private void handleGet(HttpExchange ex) {
        String query   = ex.getRequestURI().getQuery();
        String coach   = getParam(query, "coach");
        String athlete = getParam(query, "athlete");

        List<Map<String, Object>> tasks;
        if (coach != null && !coach.isBlank()) {
            tasks = dao.getTasksByCoach(coach);
        } else if (athlete != null && !athlete.isBlank()) {
            tasks = dao.getTasksByAthlete(athlete);
        } else {
            send(ex, 400, fmt.formatError("Provide coach or athlete query param"));
            return;
        }
        send(ex, 200, tasksToJson(tasks));
    }

    private void handleCreate(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String coach       = jsonStr(body, "coach");
        String athlete     = jsonStr(body, "athlete");
        String title       = jsonStr(body, "title");
        String description = jsonStr(body, "description");
        String dueDate     = jsonStr(body, "dueDate");

        if (coach == null || coach.isBlank())   { send(ex, 400, fmt.formatError("Coach email required")); return; }
        if (athlete == null || athlete.isBlank()){ send(ex, 400, fmt.formatError("Athlete email required")); return; }
        if (title == null || title.isBlank())   { send(ex, 400, fmt.formatError("Task title required")); return; }

        int id = dao.insertTask(coach, athlete,
                                title.trim(),
                                description != null ? description : "",
                                dueDate     != null ? dueDate     : "");
        if (id < 0) { send(ex, 500, fmt.formatError("Failed to save task")); return; }
        send(ex, 200, "{\"success\":true,\"id\":" + id + "}");
    }

    private void handleUpdate(HttpExchange ex) throws IOException {
        String body   = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String idStr  = jsonStr(body, "id");
        String action = jsonStr(body, "action");

        if (idStr == null || action == null) { send(ex, 400, fmt.formatError("id and action required")); return; }
        if (!action.equals("completed") && !action.equals("missed") && !action.equals("snoozed")) {
            send(ex, 400, fmt.formatError("action must be completed, missed, or snoozed")); return;
        }

        int taskId;
        try { taskId = Integer.parseInt(idStr.trim()); } catch (NumberFormatException e) {
            send(ex, 400, fmt.formatError("Invalid task id")); return;
        }

        String finalStatus = dao.updateTaskStatus(taskId, action);
        if (finalStatus == null) { send(ex, 404, fmt.formatError("Task not found")); return; }
        // Re-fetch updated snooze_count for the response
        int snoozeCount = dao.getSnoozeCount(taskId);
        send(ex, 200, "{\"success\":true,\"status\":\"" + RecordFormatter.jsonEscape(finalStatus) +
                      "\",\"snoozeCount\":" + snoozeCount + "}");
    }

    // ── JSON helpers ──────────────────────────────────────────────

    private String tasksToJson(List<Map<String, Object>> tasks) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < tasks.size(); i++) {
            Map<String, Object> t = tasks.get(i);
            sb.append("{")
              .append("\"id\":").append(t.get("id")).append(",")
              .append("\"coach\":\"").append(RecordFormatter.jsonEscape((String) t.get("coach"))).append("\",")
              .append("\"athlete\":\"").append(RecordFormatter.jsonEscape((String) t.get("athlete"))).append("\",")
              .append("\"title\":\"").append(RecordFormatter.jsonEscape((String) t.get("title"))).append("\",")
              .append("\"description\":\"").append(RecordFormatter.jsonEscape((String) t.get("description"))).append("\",")
              .append("\"dueDate\":\"").append(RecordFormatter.jsonEscape((String) t.get("dueDate"))).append("\",")
              .append("\"status\":\"").append(RecordFormatter.jsonEscape((String) t.get("status"))).append("\",")
              .append("\"snoozeCount\":").append(t.get("snoozeCount")).append(",")
              .append("\"createdAt\":\"").append(RecordFormatter.jsonEscape((String) t.get("createdAt"))).append("\"")
              .append("}");
            if (i < tasks.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String getParam(String query, String key) {
        if (query == null) return null;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                try { return java.net.URLDecoder.decode(kv[1], "UTF-8"); }
                catch (Exception e) { return kv[1]; }
            }
        }
        return null;
    }

    private String jsonStr(String body, String key) {
        String s = "\"" + key + "\""; int i = body.indexOf(s); if (i == -1) return null;
        int c = body.indexOf(":", i); if (c == -1) return null;
        int o = body.indexOf("\"", c + 1); if (o == -1) return null;
        int cl = body.indexOf("\"", o + 1);
        return cl == -1 ? null : body.substring(o + 1, cl);
    }

    private void send(HttpExchange ex, int code, String body) {
        try {
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(code, b.length);
            OutputStream os = ex.getResponseBody(); os.write(b); os.close();
        } catch (Exception ignored) {}
    }
}
