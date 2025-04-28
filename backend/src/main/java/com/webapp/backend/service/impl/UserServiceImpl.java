package com.webapp.backend.service.impl;

import com.webapp.backend.model.User;
import com.webapp.backend.model.enumeration.UserRole;
import com.webapp.backend.repository.UserRepository;
import com.webapp.backend.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;


    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Override
    public Optional<List<User>> getUsers() {
        return Optional.of(this.userRepository.findAll());
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return Optional.ofNullable(this.userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found!")));
    }

    @Override
    public Optional<User> getCurrentUser() {
        return Optional.empty();
    }

    @Override
    public Optional<User> createUser(String name, String email, String password, UserRole role) {
        User user = new User(name, email, this.bCryptPasswordEncoder.encode(password), role);
        return Optional.ofNullable(Optional.of(this.userRepository.save(user)).orElseThrow(() -> new RuntimeException("Failed to save user to database!")));
    }

    @Override
    public Optional<User> updateUser(Long id, String name, String email, String password, UserRole role) {
        User user = this.getUserById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        return Optional.ofNullable(Optional.of(this.userRepository.save(user)).orElseThrow(() -> new RuntimeException("Failed to update user!")));
    }

    @Override
    public Optional<User> deleteUser(Long id) {
        User user = this.getUserById(id).orElseThrow(() -> new RuntimeException("User not found"));
        this.userRepository.delete(user);
        return Optional.of(user);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        User user = this.userRepository.findUserByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return Optional.ofNullable(user);
    }
}
