package com.tracker.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tracker.dao.PerformanceDAO;
import com.tracker.service.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * POST /save
 * Reads sport/distance/time/athlete/m1/m2 → validates → calculates speed →
 * blends speed efficiency with coach-entered skill metrics (m1, m2) using
 * per-sport weights → saves to SQLite → generates efficiency report → returns JSON.
 *
 * m1 and m2 are optional coach-entered skill scores (0–100) whose meaning
 * differs per sport (e.g. Sprint Form for Running, Shooting Efficiency for
 * Basketball).  If omitted they default to 0.
 * Speed is always auto-derived from distance/time; its contribution to the
 * final score is controlled by the per-sport weight in PerformanceService.
 */
public class PerformanceController implements HttpHandler {

    private final PerformanceService ps  = new PerformanceService();
    private final ValidationService  vs  = new ValidationService();
    private final PerformanceLevel   pl  = new PerformanceLevel();
    private final PerformanceDAO     dao = new PerformanceDAO();
    private final RecordFormatter    fmt = new RecordFormatter();

    @Override
    public void handle(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin","*");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers","Content-Type");
        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { send(ex,204,""); return; }
        if (!ex.getRequestMethod().equalsIgnoreCase("POST"))   { send(ex,405,fmt.formatError("Method not allowed")); return; }
        try {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("[PerformanceController] " + body);

            String athlete = jsonStr(body,"athlete");
            String sport   = jsonStr(body,"sport");
            double distance= jsonNum(body,"distance");
            double timeSec = jsonNum(body,"time");

            if (athlete==null||athlete.isBlank()) athlete="Unknown";
            if (sport==null||sport.isBlank())     sport="Running";
            if (distance<=0) { send(ex,400,fmt.formatError("Distance must be > 0")); return; }
            if (timeSec<=0)  { send(ex,400,fmt.formatError("Time must be > 0")); return; }

            double speed = distance / timeSec;

            if (!vs.validateSpeedForSport(speed, sport)) {
                send(ex,400,fmt.formatError("Speed out of range for this sport")); return;
            }

            // m1 and m2 are optional coach-entered skill metrics (0–100).
            // Default to 0 when not provided (backwards-compatible).
            double m1 = jsonNumOpt(body, "m1", 0.0);
            double m2 = jsonNumOpt(body, "m2", 0.0);
            if (!vs.validateMetric(m1)) { send(ex,400,fmt.formatError("m1 must be between 0 and 100")); return; }
            if (!vs.validateMetric(m2)) { send(ex,400,fmt.formatError("m2 must be between 0 and 100")); return; }

            double score = ps.calculateScore(speed, m1, m2, sport);
            String level = pl.getLevel(score);

            // Store m1 in accuracy column, m2 in stamina column (schema unchanged)
            dao.insertRecord(athlete, sport, distance, timeSec, speed, m1, m2, score, level);

            // Generate accuracy report after insert (getAllScores now includes new row)
            PerformanceService.AccuracyReport report = ps.generateReport(speed, m1, m2, sport, score, athlete);

            send(ex, 200, fmt.formatSaveResponseWithReport(athlete, sport, speed, m1, m2, score, level, report));
            System.out.println("[PerformanceController] sport="+sport+" Score="+score+" Level="+level);

        } catch (NumberFormatException e) {
            send(ex,400,fmt.formatError("Invalid number in request"));
        } catch (Exception e) {
            System.err.println("[PerformanceController] "+e.getMessage());
            send(ex,500,fmt.formatError("Server error"));
        }
    }

    private double jsonNum(String body, String key) {
        String s="\""+key+"\""; int i=body.indexOf(s); if(i==-1) throw new NumberFormatException("Missing: "+key);
        int c=body.indexOf(":",i); if(c==-1) throw new NumberFormatException("Malformed JSON for: "+key);
        int end=body.indexOf(",",c); if(end==-1) end=body.indexOf("}",c);
        if(end==-1) throw new NumberFormatException("Malformed JSON for: "+key);
        return Double.parseDouble(body.substring(c+1,end).trim());
    }

    private double jsonNumOpt(String body, String key, double defaultVal) {
        try { return jsonNum(body, key); } catch (NumberFormatException e) { return defaultVal; }
    }

    private String jsonStr(String body, String key) {
        String s="\""+key+"\""; int i=body.indexOf(s); if(i==-1) return null;
        int c=body.indexOf(":",i); if(c==-1) return null;
        int o=body.indexOf("\"",c+1); if(o==-1) return null;
        int cl=body.indexOf("\"",o+1);
        return cl==-1 ? null : body.substring(o+1,cl);
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