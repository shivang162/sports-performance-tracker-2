package com.tracker.service;

import com.tracker.dao.UserDAO;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    public boolean login(String email, String password) {
        if (email == null || password == null) return false;
        return userDAO.validateUser(email, password);
    }
}