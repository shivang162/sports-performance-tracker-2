package com.tracker.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tracker.service.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * GET /dashboard?athlete=<email>
 * Returns: average, trend, improvement, level, totalSessions, scores[], suggestion
 * All stats are scoped to the athlete specified in the query parameter.
 */
public class DashboardController implements HttpHandler {

    private final PerformanceService ps  = new PerformanceService();
    private final SuggestionService  ss  = new SuggestionService();
    private final RecordFormatter    fmt = new RecordFormatter();

    @Override
    public void handle(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin","*");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers","Content-Type");
        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { send(ex,204,""); return; }
        if (!ex.getRequestMethod().equalsIgnoreCase("GET"))    { send(ex,405,fmt.formatError("Method not allowed")); return; }
        try {
            String athlete = queryParam(ex.getRequestURI().getRawQuery(), "athlete");
            if (athlete == null || athlete.isBlank()) {
                send(ex, 400, fmt.formatError("Missing athlete parameter")); return;
            }
            PerformanceService.DashboardStats stats = ps.getDashboardStats(athlete);
            String suggestion = ss.getSuggestion(stats.scores);
            String response   = fmt.formatDashboard(
                stats.avg, stats.trend, stats.improvement,
                stats.level, stats.sessions, stats.scores, suggestion);
            System.out.println("[DashboardController] athlete="+athlete+" avg="+String.format("%.2f",stats.avg)+" trend="+stats.trend);
            send(ex,200,response);
        } catch (Exception e) {
            System.err.println("[DashboardController] "+e.getMessage());
            send(ex,500,fmt.formatError("Could not load dashboard"));
        }
    }

    /** Extracts a named parameter from a raw query string, e.g. "athlete=foo&x=1" */
    private String queryParam(String rawQuery, String name) {
        if (rawQuery == null) return null;
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq == -1) continue;
            try {
                String k = java.net.URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                if (k.equals(name)) {
                    return java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                }
            } catch (Exception e) { System.err.println("[DashboardController] URL decode error: " + e.getMessage()); }
        }
        return null;
    }

    private void send(HttpExchange ex, int code, String body) {
        try {
            byte[] b=body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type","application/json");
            ex.sendResponseHeaders(code,b.length);
            OutputStream os=ex.getResponseBody(); os.write(b); os.close();
        } catch (Exception ignored) {}
    }
}
