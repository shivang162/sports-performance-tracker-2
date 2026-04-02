package com.tracker.service;

import java.util.List;

/** Builds JSON strings for all HTTP responses */
public class RecordFormatter {

    public String formatSaveResponse(String athlete, double speed,
                                     double accuracy, double stamina,
                                     double score, String level) {
        return String.format(
            "{\"success\":true,\"athlete\":\"%s\"," +
            "\"speed\":%.2f,\"accuracy\":%.2f,\"stamina\":%.2f," +
            "\"score\":%.2f,\"level\":\"%s\"}",
            athlete, speed, accuracy, stamina, score, level);
    }

    public String formatDashboard(double avg, String trend, double imp,
                                  String level, int sessions,
                                  List<Double> scores, String suggestion) {
        StringBuilder arr = new StringBuilder("[");
        for (int i=0; i<scores.size(); i++) {
            arr.append(String.format("%.2f", scores.get(i)));
            if (i < scores.size()-1) arr.append(",");
        }
        arr.append("]");
        String safeSuggestion = suggestion.replace("\"","'");
        return String.format(
            "{\"summary\":{\"average\":%.2f,\"trend\":\"%s\"," +
            "\"improvement\":%.1f,\"level\":\"%s\",\"totalSessions\":%d}," +
            "\"scores\":%s,\"suggestion\":\"%s\"}",
            avg, trend, imp, level, sessions, arr, safeSuggestion);
    }

    public String formatError(String msg) {
        return "{\"success\":false,\"error\":\"" + msg + "\"}";
    }

    public String formatSuccess(String msg) {
        return "{\"success\":true,\"message\":\"" + msg + "\"}";
    }
}