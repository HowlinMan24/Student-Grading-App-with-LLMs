package com.webapp.backend.service;

import com.webapp.backend.model.User;
import com.webapp.backend.model.enumeration.UserRole;

import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Optional;

public interface UserService {

    Optional<List<User>> getUsers();

    Optional<User> getUserById(Long id);

    Optional<User> getCurrentUser();

    Optional<User> createUser(String name, String email, String password, UserRole role);

    Optional<User> updateUser(Long id, String name, String email, String password, UserRole role);

    Optional<User> deleteUser(Long id);

    Optional<User> getUserByEmail(String email);
}
