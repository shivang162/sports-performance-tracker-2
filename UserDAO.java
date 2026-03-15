package com.tracker.dao;

public class UserDAO {
    public boolean validateUser(String email, String password) {
        if (email == null || password == null) {
            return false;
        }

        return email.equals("coach@example.com") && password.equals("safePassword123");
    }
}
