package com.tracker.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tracker.dao.UserDAO;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** POST /register → UserDAO.insertUser() → MySQL */
public class RegisterController implements HttpHandler {

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void handle(HttpExchange ex) {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin","*");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers","Content-Type");
        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) { send(ex,204,""); return; }
        if (!ex.getRequestMethod().equalsIgnoreCase("POST"))   { send(ex,405,err("Method not allowed")); return; }
        try {
            String body  = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String email = json(body,"email"), pass = json(body,"password"), role = json(body,"role");

            if (email==null||email.isBlank())  { send(ex,400,err("Email is required")); return; }
            if (!email.contains("@"))           { send(ex,400,err("Invalid email format")); return; }
            if (pass==null||pass.length()<6)    { send(ex,400,err("Password must be at least 6 characters")); return; }

            if (role==null||role.isBlank()) role = "athlete";

            boolean ok = userDAO.insertUser(email, pass, role);
            if (ok) send(ex,200,"{\"success\":true,\"message\":\"Registration successful\"}");
            else    send(ex,409,err("Email already registered"));
        } catch (Exception e) { send(ex,500,err("Server error")); }
    }

    private String json(String body, String key) {
        String s="\""+key+"\""; int i=body.indexOf(s); if(i==-1) return null;
        int c=body.indexOf(":",i+s.length()); if(c==-1) return null;
        int o=body.indexOf("\"",c+1); if(o==-1) return null;
        int cl=body.indexOf("\"",o+1);
        return cl==-1 ? null : body.substring(o+1,cl);
    }

    private String err(String msg) { return "{\"success\":false,\"error\":\""+msg+"\"}"; }

    private void send(HttpExchange ex, int code, String body) {
        try {
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type","application/json");
            ex.sendResponseHeaders(code, b.length);
            OutputStream os=ex.getResponseBody(); os.write(b); os.close();
        } catch (Exception ignored) {}
    }
}