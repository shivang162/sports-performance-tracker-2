package com.tracker.dao;

public class UserDAO {

    public boolean validateUser(String email, String password) {
        if (email == null || password == null) {
            return false;
        }

        // In a real app this would query a datastore. For now, use a placeholder rule.
        return email.equals("coach@example.com") && password.equals("safePassword123");
    }
}
