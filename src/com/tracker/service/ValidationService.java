package com.tracker.service;

public class ValidationService {

    public boolean validateSpeed(double speed)       { return speed >= 0 && speed <= 200; }
    public boolean validateAccuracy(double accuracy) { return accuracy >= 0 && accuracy <= 100; }
    public boolean validateStamina(double stamina)   { return stamina >= 0 && stamina <= 100; }
}