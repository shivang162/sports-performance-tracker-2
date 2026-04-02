package com.tracker.service;

public class PerformanceLevel {

    public String getLevel(double score) {
        if (score >= 85) return "Excellent";
        if (score >= 70) return "Good";
        if (score >= 50) return "Average";
        return "Needs Improvement";
    }
}