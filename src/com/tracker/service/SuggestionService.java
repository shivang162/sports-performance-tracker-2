package com.tracker.service;

import java.util.List;

/** Rule-based training suggestion engine */
public class SuggestionService {

    private static final double EXCELLENT_THRESHOLD = 85.0;

    private final PerformanceService ps = new PerformanceService();
    private final PerformanceLevel   pl = new PerformanceLevel();

    public String getSuggestion(List<Double> scores) {
        if (scores == null || scores.isEmpty())
            return "Start logging sessions to get personalised training tips!";

        String trend = ps.detectTrend(scores);
        double avg   = ps.calculateAverage(scores);
        String level = pl.getLevel(avg);

        if (trend.equals("Declining") && level.equals("Needs Improvement"))
            return "Performance is dropping. Rest for 1 week, then rebuild with low-intensity drills.";
        if (trend.equals("Declining"))
            return "Trend is declining. Add 2 recovery days per week and review your stamina training.";
        if (trend.equals("Stable") && level.equals("Excellent"))
            return "You are at Excellent level. Maintain consistency and target higher competition difficulty.";
        if (trend.equals("Stable") && level.equals("Good"))
            return "Steady at Good. Push stamina drills harder to break into Excellent territory.";
        if (trend.equals("Stable") && level.equals("Average"))
            return "Performance is stable but below Good. Focus on accuracy and stamina consistency.";
        if (trend.equals("Stable") && level.equals("Needs Improvement"))
            return "Stable but needs work. Add structured interval training 3 days per week.";
        if (trend.equals("Improving")) {
            double gap = EXCELLENT_THRESHOLD - avg;
            if (gap <= 0) return "Outstanding! You have reached Excellent level. Keep it up!";
            return String.format("Great progress! You are Improving. %.1f more points to reach Excellent.", gap);
        }
        return "Keep training consistently. Regular sessions give more accurate trend analysis.";
    }
}
