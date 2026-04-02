package com.javacravio.cravio.user.service;

import com.javacravio.cravio.common.exception.BusinessException;
import com.javacravio.cravio.common.exception.UnauthorizedException;
import com.javacravio.cravio.security.JwtService;
import com.javacravio.cravio.user.dto.AuthResponse;
import com.javacravio.cravio.user.dto.LoginRequest;
import com.javacravio.cravio.user.dto.RegisterRequest;
import com.javacravio.cravio.user.dto.UserResponse;
import com.javacravio.cravio.user.model.User;
import com.javacravio.cravio.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new BusinessException("Email already exists");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(request.role());

        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getEmail(), saved.getFullName(), saved.getRole());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        boolean validPassword = passwordEncoder.matches(request.password(), user.getPassword());
        if (!validPassword && request.password().equals(user.getPassword())) {
            // One-time migration path for any legacy plaintext password rows.
            user.setPassword(passwordEncoder.encode(request.password()));
            userRepository.save(user);
            validPassword = true;
        }

        if (!validPassword) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(token, new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole()));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}

