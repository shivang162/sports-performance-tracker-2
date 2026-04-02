package com.tracker.service;

import java.util.List;

/** Builds JSON strings for all HTTP responses */
public class RecordFormatter {

    /** Escapes a string for safe embedding inside a JSON double-quoted value. */
    public static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("/", "\\/")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public String formatSaveResponse(String athlete, double speed,
                                     double accuracy, double stamina,
                                     double score, String level) {
        return String.format(
            "{\"success\":true,\"athlete\":\"%s\"," +
            "\"speed\":%.2f,\"accuracy\":%.2f,\"stamina\":%.2f," +
            "\"score\":%.2f,\"level\":\"%s\"}",
            jsonEscape(athlete), speed, accuracy, stamina, score, jsonEscape(level));
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
        return String.format(
            "{\"summary\":{\"average\":%.2f,\"trend\":\"%s\"," +
            "\"improvement\":%.1f,\"level\":\"%s\",\"totalSessions\":%d}," +
            "\"scores\":%s,\"suggestion\":\"%s\"}",
            avg, jsonEscape(trend), imp, jsonEscape(level), sessions, arr, jsonEscape(suggestion));
    }

    public String formatError(String msg) {
        return "{\"success\":false,\"error\":\"" + jsonEscape(msg) + "\"}";
    }

    public String formatSuccess(String msg) {
        return "{\"success\":true,\"message\":\"" + jsonEscape(msg) + "\"}";
    }
}