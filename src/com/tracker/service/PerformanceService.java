package com.tracker.service;

import com.tracker.dao.PerformanceDAO;
import java.util.List;
import java.util.Map;

/**
 * Core logic — Team Lead's main file.
 * calculateScore, calculateAverage, detectTrend,
 * calculatePeriodImprovement, getDashboardStats
 */
public class PerformanceService {

    private final PerformanceDAO  dao   = new PerformanceDAO();
    private final PerformanceLevel lvl  = new PerformanceLevel();

    /**
     * Sport-specific scoring weights: speed%, accuracy%, stamina%.
     * Running:       speed dominant (60/20/20)
     * Swimming:      balanced      (40/30/30)
     * Basketball:    accuracy key  (20/50/30)
     * Football:      balanced      (35/35/30)
     * Tennis:        accuracy key  (25/45/30)
     * Cycling:       speed+stamina (60/10/30)
     * Weightlifting: form+stamina  (20/40/40)
     * Default:       balanced      (40/30/30)
     */
    private static final Map<String, double[]> SPORT_WEIGHTS = Map.of(
        "Running",       new double[]{0.60, 0.20, 0.20},
        "Swimming",      new double[]{0.40, 0.30, 0.30},
        "Basketball",    new double[]{0.20, 0.50, 0.30},
        "Football",      new double[]{0.35, 0.35, 0.30},
        "Tennis",        new double[]{0.25, 0.45, 0.30},
        "Cycling",       new double[]{0.60, 0.10, 0.30},
        "Weightlifting", new double[]{0.20, 0.40, 0.40}
    );

    // 1. Weighted score using sport-specific weights
    public double calculateScore(double speed, double accuracy, double stamina, String sport) {
        double[] w = SPORT_WEIGHTS.getOrDefault(sport, new double[]{0.40, 0.30, 0.30});
        return (speed * w[0]) + (accuracy * w[1]) + (stamina * w[2]);
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

    // 5. Full dashboard stats from DB for a specific athlete + sport
    public DashboardStats getDashboardStats(String athlete, String sport) {
        List<Double> scores = dao.getAllScores(athlete, sport);
        double avg  = calculateAverage(scores);
        String trend= detectTrend(scores);
        double imp  = calculatePeriodImprovement(scores);
        int    cnt  = dao.getTotalCount(athlete, sport);
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
