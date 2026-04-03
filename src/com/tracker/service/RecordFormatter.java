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

    /**
     * Save response that includes a full accuracy report for the coach to review.
     * m1 / m2 correspond to the sport-specific metrics (stored in accuracy/stamina columns).
     */
    public String formatSaveResponseWithReport(String athlete, String sport,
                                               double speed, double m1, double m2,
                                               double score, String level,
                                               PerformanceService.AccuracyReport rpt) {
        return String.format(
            "{\"success\":true,\"athlete\":\"%s\",\"sport\":\"%s\"," +
            "\"speed\":%.2f,\"m1\":%.2f,\"m2\":%.2f," +
            "\"score\":%.2f,\"level\":\"%s\"," +
            "\"report\":{\"speedNorm\":%.1f,\"speedContrib\":%.1f," +
            "\"m1Contrib\":%.1f,\"m2Contrib\":%.1f," +
            "\"hasPrev\":%b,\"prevScore\":%.1f,\"scoreChange\":%.1f," +
            "\"pointsToNext\":%.1f,\"nextLevel\":\"%s\",\"sessionCount\":%d}}",
            jsonEscape(athlete), jsonEscape(sport),
            speed, m1, m2, score, jsonEscape(level),
            rpt.speedNorm, rpt.speedContrib,
            rpt.m1Contrib, rpt.m2Contrib,
            rpt.hasPrev, rpt.prevScore, rpt.scoreChange,
            rpt.pointsToNext, jsonEscape(rpt.nextLevel), rpt.sessionCount);
    }

    // Kept for backwards compatibility
    public String formatSaveResponse(String athlete, String sport, double speed,
                                     double accuracy, double stamina,
                                     double score, String level) {
        return String.format(
            "{\"success\":true,\"athlete\":\"%s\",\"sport\":\"%s\"," +
            "\"speed\":%.2f,\"accuracy\":%.2f,\"stamina\":%.2f," +
            "\"score\":%.2f,\"level\":\"%s\"}",
            jsonEscape(athlete), jsonEscape(sport), speed, accuracy, stamina, score, jsonEscape(level));
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