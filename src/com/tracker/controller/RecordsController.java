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
 * Returns all performance records with athlete name as a JSON array.
 * Used by compare.html to filter records per athlete.
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
            List<Object[]> records = dao.getAllRecordsWithAthlete();
            StringBuilder json = new StringBuilder("[");
            for (int i=0; i<records.size(); i++) {
                Object[] r = records.get(i);
                // {athlete, distance, time_sec, speed, accuracy, stamina, score, level}
                json.append(String.format(
                    "{\"athlete\":\"%s\",\"distance\":%.2f,\"time\":%.2f," +
                    "\"speed\":%.2f,\"accuracy\":%.2f,\"stamina\":%.2f," +
                    "\"score\":%.2f,\"level\":\"%s\"}",
                    RecordFormatter.jsonEscape((String)r[0]),r[1],r[2],r[3],r[4],r[5],r[6],RecordFormatter.jsonEscape((String)r[7])));
                if (i < records.size()-1) json.append(",");
            }
            json.append("]");
            send(ex,200,json.toString());
        } catch (Exception e) {
            System.err.println("[RecordsController] "+e.getMessage());
            send(ex,500,"{\"error\":\"Could not load records\"}");
        }
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