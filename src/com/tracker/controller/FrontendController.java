package com.tracker.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Serves the bundled index.html at GET / so the app opens directly
 * in the browser when the server is running at http://localhost:8080.
 */
public class FrontendController implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        InputStream in = getClass().getResourceAsStream("/frontend/index.html");
        if (in == null) {
            byte[] msg = "Frontend not found.".getBytes();
            exchange.sendResponseHeaders(404, msg.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(msg);
            }
            return;
        }

        byte[] body;
        try (InputStream resource = in) {
            body = resource.readAllBytes();
        }

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
