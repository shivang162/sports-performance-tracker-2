package com.tracker.service;

import java.util.List;

/** Rule-based training suggestion engine */
public class SuggestionService {

    private static final double EXCELLENT_THRESHOLD = 85.0;

    private final PerformanceService ps = new PerformanceService();
    private final PerformanceLevel   pl = new PerformanceLevel();

    public String getSuggestion(List<Double> scores, String sport) {
        if (scores == null || scores.isEmpty())
            return "Start logging " + sport + " sessions to get personalised training tips!";

        String trend = ps.detectTrend(scores);
        double avg   = ps.calculateAverage(scores);
        String level = pl.getLevel(avg);

        String sportTip = getSportTip(sport);

        if (trend.equals("Declining") && level.equals("Needs Improvement"))
            return "Performance is dropping. Rest for 1 week, then rebuild with low-intensity " + sport.toLowerCase() + " drills.";
        if (trend.equals("Declining"))
            return "Trend is declining. Add 2 recovery days per week and review your " + sport.toLowerCase() + " stamina training.";
        if (trend.equals("Stable") && level.equals("Excellent"))
            return "You are at Excellent level in " + sport + ". Maintain consistency and target higher competition difficulty.";
        if (trend.equals("Stable") && level.equals("Good"))
            return "Steady at Good in " + sport + ". " + sportTip;
        if (trend.equals("Stable") && level.equals("Average"))
            return "Performance is stable but below Good. Focus on " + sport.toLowerCase() + " accuracy and stamina consistency.";
        if (trend.equals("Stable") && level.equals("Needs Improvement"))
            return "Stable but needs work. Add structured " + sport.toLowerCase() + " interval training 3 days per week.";
        if (trend.equals("Improving")) {
            double gap = EXCELLENT_THRESHOLD - avg;
            if (gap <= 0) return "Outstanding! You have reached Excellent level in " + sport + ". Keep it up!";
            return String.format("Great progress in %s! You are Improving. %.1f more points to reach Excellent.", sport, gap);
        }
        return "Keep training consistently. Regular " + sport.toLowerCase() + " sessions give more accurate trend analysis.";
    }

    /** Returns a sport-specific push tip used when athlete is at Good/Stable level */
    private String getSportTip(String sport) {
        switch (sport) {
            case "Running":       return "Push your interval pace and add hill runs to break into Excellent.";
            case "Swimming":      return "Work on stroke technique and flip-turns to improve your speed score.";
            case "Basketball":    return "Focus on high-repetition shooting drills to lift your shot accuracy.";
            case "Football":      return "Increase passing accuracy drills and add sprint intervals for speed.";
            case "Tennis":        return "Drill your serve placement and footwork to boost shot accuracy.";
            case "Cycling":       return "Add tempo rides and hill climbs to raise your speed and stamina scores.";
            case "Weightlifting": return "Prioritise form consistency and progressive overload to reach Excellent.";
            default:              return "Push your key metrics harder to break into Excellent territory.";
        }
    }
}
