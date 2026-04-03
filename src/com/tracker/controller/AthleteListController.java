package com.tracker.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tracker.dao.UserDAO;
import com.tracker.service.RecordFormatter;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * GET /athletes
 * Returns a JSON array of all registered athlete emails so that the coach's
 * "Add Performance" form can offer a pick-list of existing athletes.
 *
 * Example response:
 *   ["alice@example.com","bob@example.com"]
 */
public class AthleteListController implements HttpHandler {

    private final UserDAO       dao = new UserDAO();
    private final RecordFormatter fmt = new RecordFormatter();

    @Override
    public void handle(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { send(ex, 204, ""); return; }
        if (!ex.getRequestMethod().equalsIgnoreCase("GET"))    { send(ex, 405, fmt.formatError("Method not allowed")); return; }

        try {
            List<String> emails = dao.listAthleteEmails();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < emails.size(); i++) {
                sb.append("\"").append(RecordFormatter.jsonEscape(emails.get(i))).append("\"");
                if (i < emails.size() - 1) sb.append(",");
            }
            sb.append("]");
            send(ex, 200, sb.toString());
        } catch (Exception e) {
            System.err.println("[AthleteListController] " + e.getMessage());
            send(ex, 500, fmt.formatError("Server error"));
        }
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
