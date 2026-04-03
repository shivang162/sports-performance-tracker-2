package com.tracker.service;

public class ValidationService {

    public boolean validateSpeed(double speed)        { return speed >= 0 && speed <= 200; }
    public boolean validateMetric(double value)       { return value >= 0 && value <= 100; }

    /** Weightlifting speed = weight/reps, which can exceed 200 for heavy single lifts. */
    public boolean validateSpeedForSport(double speed, String sport) {
        if ("Weightlifting".equals(sport)) return speed >= 0 && speed <= 2000;
        return validateSpeed(speed);
    }

    // Kept for backwards compatibility with any existing callers
    public boolean validateAccuracy(double accuracy) { return validateMetric(accuracy); }
    public boolean validateStamina(double stamina)   { return validateMetric(stamina); }
}