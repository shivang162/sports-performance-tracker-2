package com.tracker.service;

import com.tracker.dao.PerformanceDAO;
import java.util.List;

/**
 * Core logic — Team Lead's main file.
 * calculateScore, calculateAverage, detectTrend,
 * calculatePeriodImprovement, getDashboardStats
 */
public class PerformanceService {

    private final PerformanceDAO  dao   = new PerformanceDAO();
    private final PerformanceLevel lvl  = new PerformanceLevel();

    // 1. Weighted score: speed 40%, accuracy 30%, stamina 30%
    public double calculateScore(double speed, double accuracy, double stamina) {
        return (speed * 0.4) + (accuracy * 0.3) + (stamina * 0.3);
    }

    // 2. Average
    public double calculateAverage(List<Double> scores) {
        if (scores == null || scores.isEmpty()) return 0.0;
        double sum = 0;
        for (double s : scores) sum += s;
        return sum / scores.size();
    }

    // 3. Trend via linear regression slope
    public String detectTrend(List<Double> scores) {
        if (scores == null || scores.size() < 2) return "Stable";
        int n = scores.size();
        double sx=0, sy=0, sxy=0, sx2=0;
        for (int i=0; i<n; i++) {
            sx+=i; sy+=scores.get(i); sxy+=i*scores.get(i); sx2+=i*i;
        }
        double d = (n*sx2)-(sx*sx);
        if (d==0) return "Stable";
        double slope = ((n*sxy)-(sx*sy))/d;
        if (slope > 0.5)  return "Improving";
        if (slope < -0.5) return "Declining";
        return "Stable";
    }

    // 4. % improvement: first-half avg vs second-half avg
    public double calculatePeriodImprovement(List<Double> scores) {
        if (scores == null || scores.size() < 2) return 0.0;
        int mid = scores.size()/2;
        double a = calculateAverage(scores.subList(0, mid));
        double b = calculateAverage(scores.subList(mid, scores.size()));
        if (a == 0) return 0.0;
        return ((b-a)/Math.abs(a))*100.0;
    }

    // 5. Full dashboard stats from DB for a specific athlete
    public DashboardStats getDashboardStats(String athlete) {
        List<Double> scores = dao.getAllScores(athlete);
        double avg  = calculateAverage(scores);
        String trend= detectTrend(scores);
        double imp  = calculatePeriodImprovement(scores);
        int    cnt  = dao.getTotalCount(athlete);
        String lev  = lvl.getLevel(avg);
        return new DashboardStats(avg, trend, imp, lev, cnt, scores);
    }

    // Inner DTO
    public static class DashboardStats {
        public final double avg; public final String trend;
        public final double improvement; public final String level;
        public final int sessions; public final List<Double> scores;
        public DashboardStats(double avg,String trend,double imp,
                              String level,int sessions,List<Double> scores) {
            this.avg=avg; this.trend=trend; this.improvement=imp;
            this.level=level; this.sessions=sessions; this.scores=scores;
        }
    }
}
