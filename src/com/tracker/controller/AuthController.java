package com.tracker.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tracker.service.AuthService;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** POST /login → AuthService → UserDAO → PostgreSQL */
public class AuthController implements HttpHandler {

    private final AuthService authService = new AuthService();
    private final com.tracker.dao.UserDAO userDAO = new com.tracker.dao.UserDAO();

    @Override
    public void handle(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers","Content-Type");
        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { send(ex,204,""); return; }
        if (!ex.getRequestMethod().equalsIgnoreCase("POST"))   { send(ex,405,"{\"success\":false,\"error\":\"Method Not Allowed\"}"); return; }
        try {
            String body  = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String email = json(body,"email"), pass = json(body,"password");
            if (authService.login(email, pass)) {
                String role = userDAO.getUserRole(email);
                send(ex, 200, "{\"success\":true,\"message\":\"Login Successful\",\"role\":\"" + role + "\"}");
            } else {
                send(ex, 401, "{\"success\":false,\"error\":\"Invalid email or password\"}");
            }
        } catch (Exception e) { send(ex,500,"{\"success\":false,\"error\":\"Internal Server Error\"}"); }
    }

    private String json(String body, String key) {
        String s = "\""+key+"\""; int i = body.indexOf(s); if(i==-1) return null;
        int c=body.indexOf(":",i+s.length()); if(c==-1) return null;
        int o=body.indexOf("\"",c+1); if(o==-1) return null;
        int cl=body.indexOf("\"",o+1);
        return cl==-1 ? null : body.substring(o+1,cl);
    }

    private void send(HttpExchange ex, int code, String body) {
        try {
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type","application/json");
            ex.sendResponseHeaders(code, b.length);
            OutputStream os = ex.getResponseBody(); os.write(b); os.close();
        } catch (Exception ignored) {}
    }
}