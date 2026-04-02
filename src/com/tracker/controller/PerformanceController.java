package com.tracker.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tracker.dao.PerformanceDAO;
import com.tracker.service.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * POST /save
 * Reads distance/time/accuracy/stamina/athlete → validates → calculates
 * speed+score+level → saves to MySQL → returns JSON
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

            String athlete  = jsonStr(body,"athlete");
            double distance = jsonNum(body,"distance");
            double timeSec  = jsonNum(body,"time");
            double accuracy = jsonNum(body,"accuracy");
            double stamina  = jsonNum(body,"stamina");

            if (athlete==null||athlete.isBlank()) athlete="Unknown";
            if (distance<=0) { send(ex,400,fmt.formatError("Distance must be > 0")); return; }
            if (timeSec<=0)  { send(ex,400,fmt.formatError("Time must be > 0")); return; }

            double speed = distance/timeSec;

            if (!vs.validateSpeed(speed))    { send(ex,400,fmt.formatError("Speed out of range 0–200")); return; }
            if (!vs.validateAccuracy(accuracy)){ send(ex,400,fmt.formatError("Accuracy must be 0–100")); return; }
            if (!vs.validateStamina(stamina)) { send(ex,400,fmt.formatError("Stamina must be 0–100")); return; }

            double score = ps.calculateScore(speed, accuracy, stamina);
            String level = pl.getLevel(score);

            dao.insertRecord(athlete, distance, timeSec, speed, accuracy, stamina, score, level);

            send(ex, 200, fmt.formatSaveResponse(athlete, speed, accuracy, stamina, score, level));
            System.out.println("[PerformanceController] Score="+score+" Level="+level);

        } catch (NumberFormatException e) {
            send(ex,400,fmt.formatError("Invalid number in request"));
        } catch (Exception e) {
            System.err.println("[PerformanceController] "+e.getMessage());
            send(ex,500,fmt.formatError("Server error: "+e.getMessage()));
        }
    }

    private double jsonNum(String body, String key) {
        String s="\""+key+"\""; int i=body.indexOf(s); if(i==-1) throw new NumberFormatException("Missing: "+key);
        int c=body.indexOf(":",i); if(c==-1) throw new NumberFormatException("Malformed JSON for: "+key);
        int end=body.indexOf(",",c); if(end==-1) end=body.indexOf("}",c);
        if(end==-1) throw new NumberFormatException("Malformed JSON for: "+key);
        return Double.parseDouble(body.substring(c+1,end).trim());
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