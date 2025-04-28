package com.webapp.backend.service.impl;

import com.webapp.backend.model.User;
import com.webapp.backend.model.enumeration.UserRole;
import com.webapp.backend.repository.UserRepository;
import com.webapp.backend.service.AuthService;
import com.webapp.backend.util.JWTUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;

    public AuthServiceImpl(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder, AuthenticationManager authenticationManager, JWTUtil jwtUtil) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public String login(String email, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getName());
        } catch (AuthenticationException e) {
            throw new RuntimeException("Invalid credentials");
        }
    }

    @Override
    public User register(String name, String email, String password, UserRole role) {
        if (userRepository.findUserByEmail(email).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        user.setRole(role);

        return userRepository.save(user);
    }
}
