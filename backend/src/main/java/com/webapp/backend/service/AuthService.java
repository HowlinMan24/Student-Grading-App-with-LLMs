package com.webapp.backend.service;

import com.webapp.backend.model.User;
import com.webapp.backend.model.enumeration.UserRole;

public interface AuthService {
    String login(String email, String password);
    User register(String name, String email, String password, UserRole role);
}
