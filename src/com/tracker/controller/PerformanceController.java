package com.tracker.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tracker.dao.PerformanceDAO;
import com.tracker.service.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * POST /save
 * Reads sport/distance/time/accuracy(=m1)/stamina(=m2)/athlete → validates →
 * calculates speed + normalised score + level (sport-specific weights) →
 * saves to SQLite → generates accuracy report → returns JSON
 *
 * The "accuracy" and "stamina" request fields now carry sport-specific metric
 * values (m1 / m2).  They are still stored in the accuracy/stamina DB columns.
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
            // "accuracy" field now carries sport-specific metric-1 value (0–100)
            // "stamina"  field now carries sport-specific metric-2 value (0–100, or 0 if unused)
            double m1      = jsonNum(body,"accuracy");
            double m2      = jsonNumOpt(body,"stamina", 0.0);

            if (athlete==null||athlete.isBlank()) athlete="Unknown";
            if (sport==null||sport.isBlank())     sport="Running";
            if (distance<=0) { send(ex,400,fmt.formatError("Distance must be > 0")); return; }
            if (timeSec<=0)  { send(ex,400,fmt.formatError("Time must be > 0")); return; }

            double speed = distance / timeSec;

            if (!vs.validateSpeedForSport(speed, sport)) {
                send(ex,400,fmt.formatError("Speed out of range for this sport")); return;
            }
            if (!vs.validateMetric(m1)) {
                send(ex,400,fmt.formatError("Metric 1 must be 0–100")); return;
            }
            if (!vs.validateMetric(m2)) {
                send(ex,400,fmt.formatError("Metric 2 must be 0–100")); return;
            }

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

    private double jsonNumOpt(String body, String key, double def) {
        try { return jsonNum(body, key); } catch (NumberFormatException e) { return def; }
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