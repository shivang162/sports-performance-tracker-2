package com.tracker;

import com.sun.net.httpserver.HttpServer;
import com.tracker.controller.*;
import com.tracker.dao.DBConnection;
import java.net.InetSocketAddress;

/**
 * Application entry point.
 *
 * Routes:
 *   POST /login     → AuthController
 *   POST /register  → RegisterController
 *   POST /save      → PerformanceController
 *   GET  /dashboard → DashboardController  (stats + suggestion)
 *   GET  /records   → RecordsController    (all records for compare page)
 *
 * How to run:
 *   1. Start MySQL
 *   2. mysql -u root -p < database/schema.sql
 *   3. Add mysql-connector-java JAR to lib/ folder
 *   4. Run Main.java
 *   5. Open src/com/tracker/frontend/login.html in browser
 */
public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println("Starting Sports Performance Tracker...");

        // Test DB connection
        try {
            DBConnection.getConnection();
            System.out.println("[Main] Database connected.");
        } catch (Exception e) {
            System.err.println("[Main] DB not available: " + e.getMessage());
            System.err.println("[Main] Server starting anyway (fallback mode active).");
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/login",     new AuthController());
        server.createContext("/register",  new RegisterController());
        server.createContext("/save",      new PerformanceController());
        server.createContext("/dashboard", new DashboardController());
        server.createContext("/records",   new RecordsController());
        server.setExecutor(null);
        server.start();

        System.out.println("\nServer running → http://localhost:8080");
        System.out.println("  POST /login     — authenticate");
        System.out.println("  POST /register  — register new user");
        System.out.println("  POST /save      — save session to DB");
        System.out.println("  GET  /dashboard — stats + suggestion");
        System.out.println("  GET  /records   — all records");
        System.out.println("\nOpen frontend/login.html in your browser.");
    }
}