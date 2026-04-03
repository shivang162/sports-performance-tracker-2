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
 *   1. make run          (compiles + starts the server)
 *   2. Open src/com/tracker/frontend/login.html in browser
 *      or navigate to http://localhost:8080
 *   The SQLite database file (sports_tracker.db) is created automatically on first run.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        System.out.println("Starting Sports Performance Tracker...");

        // Test DB connection at startup
        try {
            DBConnection.getConnection().close();
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
        server.createContext("/athletes",  new AthleteListController());
        server.setExecutor(null);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            System.out.println("[Main] Server stopped.");
        }));

        System.out.println("\nServer running → http://localhost:8080");
        System.out.println("  POST /login     — authenticate");
        System.out.println("  POST /register  — register new user");
        System.out.println("  POST /save      — save session to DB");
        System.out.println("  GET  /dashboard — stats + suggestion");
        System.out.println("  GET  /records   — all records");
        System.out.println("  GET  /athletes  — list athlete accounts");
        System.out.println("\nOpen frontend/login.html in your browser.");
    }
}