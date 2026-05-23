package com.javacravio.cravio.user.controller;

import com.javacravio.cravio.common.dto.ApiResponse;
import com.javacravio.cravio.common.exception.UnauthorizedException;
import com.javacravio.cravio.user.dto.AuthResponse;
import com.javacravio.cravio.user.dto.LoginRequest;
import com.javacravio.cravio.user.dto.RegisterRequest;
import com.javacravio.cravio.user.dto.UserResponse;
import com.javacravio.cravio.user.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered", userService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("AuthController.login entered");
        log.info("Login request received for email={}", request.email());

        String normalizedEmail = normalizeEmail(request.email());

        try {
            log.info("Before authenticationManager.authenticate for email={}", normalizedEmail);
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );
            log.info("Authentication successful for email={}", normalizedEmail);

            LoginRequest normalizedRequest = new LoginRequest(normalizedEmail, request.password());
            AuthResponse response = userService.login(normalizedRequest);
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (AuthenticationException ex) {
            ex.printStackTrace();
            log.warn("Invalid credentials for email={}", normalizedEmail);
            throw new UnauthorizedException("Invalid credentials");
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("Unexpected error during login for email={}", normalizedEmail, ex);
            throw ex;
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}

