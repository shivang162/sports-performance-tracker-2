import com.tracker.service.*;
import java.util.*;

/**
 * Tests all Team Lead logic modules.
 *
 * Compile & run (from project root):
 *   javac -d out src/com/tracker/service/*.java src/com/tracker/dao/*.java TestLogic.java
 *   java -cp out TestLogic
 */
public class TestLogic {

    public static void main(String[] args) {

        PerformanceService  ps  = new PerformanceService();
        PerformanceAnalyzer pa  = new PerformanceAnalyzer();
        SuggestionService   ss  = new SuggestionService();
        RecordFormatter     fmt = new RecordFormatter();

        List<Double> improving = Arrays.asList(60.0, 70.0, 80.0, 90.0);
        List<Double> declining = Arrays.asList(90.0, 80.0, 70.0, 60.0);

        System.out.println("=== PerformanceService ===");
        System.out.println("1. calculateScore(Running,90,50,75)    = " + ps.calculateScore(90,50,75,"Running"));
        // Running weights 60/40/0, maxSpeed=10.5 → normSpd=min(90/10.5*100,100)=100
        // expected: (100*0.6)+(50*0.4)+(75*0.0) = 60+20+0 = 80.0
        System.out.println("   calculateScore(Basketball,90,50,75) = " + ps.calculateScore(90,50,75,"Basketball"));
        // Basketball weights 0/55/45, maxSpeed=1.0 → normSpd=min(90/1.0*100,100)=100
        // expected: (100*0.0)+(50*0.55)+(75*0.45) = 0+27.5+33.75 = 61.25

        System.out.println("2. calculateAverage            = " + ps.calculateAverage(improving));
        // Expected: 75.0

        System.out.println("3. detectTrend (improving)     = " + ps.detectTrend(improving));
        // Expected: Improving

        System.out.println("4. detectTrend (declining)     = " + ps.detectTrend(declining));
        // Expected: Declining

        System.out.println("5. detectTrend (stable)        = " + ps.detectTrend(Arrays.asList(70.0,71.0,70.5)));
        // Expected: Stable

        System.out.println("6. periodImprovement           = " + String.format("%.1f%%", ps.calculatePeriodImprovement(improving)));
        // Expected: ~30.8%

        System.out.println("\n=== PerformanceAnalyzer ===");
        System.out.println("7. analyzeLevel                = " + pa.analyzeLevel(improving));
        // Expected: Good

        System.out.println("8. gapToNextLevel              = " + pa.gapToNextLevel(improving));
        // Expected: You need 10.0 more points to reach Excellent.

        System.out.println("9. compareAthletesJson         = " + pa.compareAthletesJson(improving,"Rahul",declining,"Amit"));

        System.out.println("\n=== SuggestionService ===");
        System.out.println("10. improving suggestion (Running)    = " + ss.getSuggestion(improving,"Running"));
        System.out.println("11. declining suggestion (Swimming)   = " + ss.getSuggestion(declining,"Swimming"));
        System.out.println("12. empty suggestion (Basketball)     = " + ss.getSuggestion(new ArrayList<>(),"Basketball"));

        System.out.println("\n=== RecordFormatter ===");
        System.out.println("13. formatSaveResponse         = " + fmt.formatSaveResponse("Rahul","Running",8.5,80,75,73.5,"Good"));
        System.out.println("14. formatError                = " + fmt.formatError("Test error"));
        System.out.println("15. formatSuccess              = " + fmt.formatSuccess("Saved OK"));

        System.out.println("\n=== ValidationService ===");
        ValidationService vs = new ValidationService();
        System.out.println("16. validateSpeed(8.5)         = " + vs.validateSpeed(8.5));   // true
        System.out.println("17. validateSpeed(250)         = " + vs.validateSpeed(250));   // false
        System.out.println("18. validateAccuracy(85)       = " + vs.validateAccuracy(85)); // true
        System.out.println("19. validateStamina(-1)        = " + vs.validateStamina(-1));  // false

        System.out.println("\n=== PerformanceLevel ===");
        PerformanceLevel pl = new PerformanceLevel();
        System.out.println("20. getLevel(90)               = " + pl.getLevel(90)); // Excellent
        System.out.println("21. getLevel(75)               = " + pl.getLevel(75)); // Good
        System.out.println("22. getLevel(55)               = " + pl.getLevel(55)); // Average
        System.out.println("23. getLevel(30)               = " + pl.getLevel(30)); // Needs Improvement

        System.out.println("\n✅ All tests complete.");
    }
}