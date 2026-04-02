package com.tracker.service;

import java.util.List;

/** Benchmark comparison and athlete comparison */
public class PerformanceAnalyzer {

    private static final double EXCELLENT = 85.0;
    private static final double GOOD      = 70.0;
    private static final double AVERAGE   = 50.0;

    private final PerformanceService ps  = new PerformanceService();
    private final PerformanceLevel   pl  = new PerformanceLevel();

    public String analyzeLevel(List<Double> scores) {
        return pl.getLevel(ps.calculateAverage(scores));
    }

    public String gapToNextLevel(List<Double> scores) {
        double avg = ps.calculateAverage(scores);
        if (avg >= EXCELLENT) return "You are at the top level: Excellent!";
        if (avg >= GOOD)      return String.format("You need %.1f more points to reach Excellent.", EXCELLENT-avg);
        if (avg >= AVERAGE)   return String.format("You need %.1f more points to reach Good.", GOOD-avg);
        return String.format("You need %.1f more points to reach Average.", AVERAGE-avg);
    }

    public String compareAthletesJson(List<Double> scA, String nA, List<Double> scB, String nB) {
        double aA = ps.calculateAverage(scA), aB = ps.calculateAverage(scB);
        String lA = pl.getLevel(aA),          lB = pl.getLevel(aB);
        String winner = aA>aB ? nA+" is performing better."
                      : aB>aA ? nB+" is performing better."
                      : "Both athletes are equal.";
        return String.format(
            "{\"athleteA\":{\"name\":\"%s\",\"average\":%.2f,\"level\":\"%s\"}," +
             "\"athleteB\":{\"name\":\"%s\",\"average\":%.2f,\"level\":\"%s\"}," +
             "\"result\":\"%s\"}",
            nA,aA,lA, nB,aB,lB, winner);
    }
}