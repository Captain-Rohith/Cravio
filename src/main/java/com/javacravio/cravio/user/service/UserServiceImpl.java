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
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setRole(request.role());

        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getEmail(), saved.getFullName(), saved.getRole());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new AuthResponse(token, new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole()));
    }
}

