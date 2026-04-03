package com.tracker.service;

import com.tracker.dao.PerformanceDAO;
import java.util.List;
import java.util.Map;

/**
 * Core logic — Team Lead's main file.
 * calculateScore, calculateAverage, detectTrend,
 * calculatePeriodImprovement, getDashboardStats, generateReport
 *
 * Scoring uses sport-specific weights [wSpeed, wM1, wM2].
 * Speed is normalised to 0–100 against a sport-specific benchmark before weighting,
 * so scores always fall in the 0–100 range.
 *
 * Sports with no meaningful speed (Basketball, Tennis) have wSpeed = 0.
 * Sports with only one extra metric (Running, Swimming, Cycling, Weightlifting)
 * have wM2 = 0 and the second metric value is ignored.
 *
 * The "accuracy" DB column stores metric-1 and "stamina" stores metric-2.
 */
public class PerformanceService {

    private final PerformanceDAO  dao   = new PerformanceDAO();
    private final PerformanceLevel lvl  = new PerformanceLevel();

    /**
     * Maximum speed benchmarks used to normalise speed to 0–100.
     * Units match whatever the sport sends as distance/time:
     *   Running/Swimming/Football → m/s
     *   Cycling                   → km/s  (coach enters km + seconds)
     *   Weightlifting             → kg/rep (weight ÷ reps)
     *   Basketball/Tennis         → not used (wSpeed = 0)
     */
    private static final Map<String, Double> SPORT_MAX_SPEED = Map.of(
        "Running",       10.5,   // 10.5 m/s ≈ elite sprint upper bound
        "Swimming",       2.5,   // 2.5 m/s ≈ elite swimmer
        "Basketball",     1.0,   // unused (wSpeed = 0)
        "Football",       3.0,   // 3 m/s average distance coverage
        "Tennis",         1.0,   // unused (wSpeed = 0)
        "Cycling",        0.015, // 0.015 km/s ≈ 54 km/h
        "Weightlifting", 25.0    // 25 kg/rep ≈ elite lifter single-rep load
    );

    /**
     * Sport-specific scoring weights: [wSpeed, wM1, wM2].
     * All weights in each row sum to 1.0.
     * M1/M2 are sport-specific metrics (0–100 range, coach-entered).
     */
    private static final Map<String, double[]> SPORT_WEIGHTS = Map.of(
        "Running",       new double[]{0.60, 0.40, 0.00},
        "Swimming",      new double[]{0.60, 0.40, 0.00},
        "Basketball",    new double[]{0.00, 0.55, 0.45},
        "Football",      new double[]{0.30, 0.40, 0.30},
        "Tennis",        new double[]{0.00, 0.55, 0.45},
        "Cycling",       new double[]{0.70, 0.30, 0.00},
        "Weightlifting", new double[]{0.40, 0.60, 0.00}
    );

    /**
     * Calculates a 0–100 performance score using normalised speed and
     * sport-specific metric values (m1, m2).
     * m1 is stored in the "accuracy" DB column; m2 in "stamina".
     */
    public double calculateScore(double speed, double m1, double m2, String sport) {
        double[] w       = SPORT_WEIGHTS.getOrDefault(sport, new double[]{0.40, 0.30, 0.30});
        double   maxSpd  = SPORT_MAX_SPEED.getOrDefault(sport, 10.0);
        double   normSpd = maxSpd > 0 ? Math.min(speed / maxSpd * 100.0, 100.0) : 0.0;
        double   raw     = (normSpd * w[0]) + (m1 * w[1]) + (m2 * w[2]);
        return Math.min(Math.max(raw, 0.0), 100.0);
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

    /**
     * Generates an accuracy report for the session that was just saved.
     * Called after insert; getAllScores will already include the new record.
     */
    public AccuracyReport generateReport(double speed, double m1, double m2,
                                         String sport, double score, String athlete) {
        double[] w      = SPORT_WEIGHTS.getOrDefault(sport, new double[]{0.40, 0.30, 0.30});
        double   maxSpd = SPORT_MAX_SPEED.getOrDefault(sport, 10.0);
        double   normSpd= maxSpd > 0 ? Math.min(speed / maxSpd * 100.0, 100.0) : 0.0;

        double speedContrib = normSpd * w[0];
        double m1Contrib    = m1     * w[1];
        double m2Contrib    = m2     * w[2];

        List<Double> allScores = dao.getAllScores(athlete, sport);
        int    total   = allScores.size();               // includes newly saved row
        boolean hasPrev= total >= 2;
        double prevScore    = hasPrev ? allScores.get(total - 2) : 0.0;
        double scoreChange  = hasPrev ? score - prevScore : 0.0;

        // Distance to next performance level
        String nextLevel;
        double pointsToNext;
        if      (score < 50) { nextLevel = "Average";   pointsToNext = 50 - score; }
        else if (score < 70) { nextLevel = "Good";      pointsToNext = 70 - score; }
        else if (score < 85) { nextLevel = "Excellent"; pointsToNext = 85 - score; }
        else                 { nextLevel = "Peak";      pointsToNext = 0; }

        return new AccuracyReport(normSpd, speedContrib, m1Contrib, m2Contrib,
                                  hasPrev, prevScore, scoreChange,
                                  pointsToNext, nextLevel, total);
    }

    // ── Inner DTOs ────────────────────────────────────────────────

    public static class AccuracyReport {
        public final double  speedNorm;
        public final double  speedContrib, m1Contrib, m2Contrib;
        public final boolean hasPrev;
        public final double  prevScore, scoreChange;
        public final double  pointsToNext;
        public final String  nextLevel;
        public final int     sessionCount;

        public AccuracyReport(double speedNorm, double speedContrib,
                              double m1Contrib, double m2Contrib,
                              boolean hasPrev, double prevScore, double scoreChange,
                              double pointsToNext, String nextLevel, int sessionCount) {
            this.speedNorm    = speedNorm;
            this.speedContrib = speedContrib;
            this.m1Contrib    = m1Contrib;
            this.m2Contrib    = m2Contrib;
            this.hasPrev      = hasPrev;
            this.prevScore    = prevScore;
            this.scoreChange  = scoreChange;
            this.pointsToNext = pointsToNext;
            this.nextLevel    = nextLevel;
            this.sessionCount = sessionCount;
        }
    }

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
