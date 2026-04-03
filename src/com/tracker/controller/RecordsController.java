package com.tracker.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tracker.dao.PerformanceDAO;
import com.tracker.service.RecordFormatter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * GET /records
 * GET /records?athlete=email   (athlete-scoped view — returns only that athlete's records)
 * Returns performance records as a JSON array.
 * Used by compare.html (no filter) and athlete.html (athlete filter).
 */
public class RecordsController implements HttpHandler {

    private final PerformanceDAO dao = new PerformanceDAO();

    @Override
    public void handle(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin","*");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers","Content-Type");
        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { send(ex,204,""); return; }
        if (!ex.getRequestMethod().equalsIgnoreCase("GET"))    { send(ex,405,"{\"error\":\"Method not allowed\"}"); return; }
        try {
            String athleteFilter = queryParam(ex.getRequestURI().getRawQuery(), "athlete");
            String sportFilter   = queryParam(ex.getRequestURI().getRawQuery(), "sport");
            List<Object[]> records;
            if (athleteFilter != null && !athleteFilter.isBlank() &&
                sportFilter   != null && !sportFilter.isBlank()) {
                records = dao.getRecordsByAthleteSport(athleteFilter, sportFilter);
            } else if (athleteFilter != null && !athleteFilter.isBlank()) {
                records = dao.getRecordsByAthlete(athleteFilter);
            } else {
                records = dao.getAllRecordsWithAthlete();
            }
            StringBuilder json = new StringBuilder("[");
            for (int i=0; i<records.size(); i++) {
                Object[] r = records.get(i);
                // {athlete, sport, distance, time_sec, speed, accuracy, stamina, score, level}
                json.append(String.format(
                    "{\"athlete\":\"%s\",\"sport\":\"%s\",\"distance\":%.2f,\"time\":%.2f," +
                    "\"speed\":%.2f,\"accuracy\":%.2f,\"stamina\":%.2f," +
                    "\"score\":%.2f,\"level\":\"%s\"}",
                    RecordFormatter.jsonEscape((String)r[0]),RecordFormatter.jsonEscape((String)r[1]),
                    r[2],r[3],r[4],r[5],r[6],r[7],RecordFormatter.jsonEscape((String)r[8])));
                if (i < records.size()-1) json.append(",");
            }
            json.append("]");
            send(ex,200,json.toString());
        } catch (Exception e) {
            System.err.println("[RecordsController] "+e.getMessage());
            send(ex,500,"{\"error\":\"Could not load records\"}");
        }
    }

    private String queryParam(String rawQuery, String name) {
        if (rawQuery == null) return null;
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq == -1) continue;
            try {
                String k = java.net.URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                if (k.equals(name)) return java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            } catch (Exception ignored) {}
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