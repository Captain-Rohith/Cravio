package com.javacravio.cravio.user.service;

import com.javacravio.cravio.common.exception.BusinessException;
import com.javacravio.cravio.common.exception.UnauthorizedException;
import com.javacravio.cravio.security.JwtService;
import com.javacravio.cravio.user.dto.LoginRequest;
import com.javacravio.cravio.user.dto.RegisterRequest;
import com.javacravio.cravio.user.model.Role;
import com.javacravio.cravio.user.model.User;
import com.javacravio.cravio.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("john@cravio.com");
        user.setPassword("encoded");
        user.setFullName("John");
        user.setRole(Role.CUSTOMER);
    }

    @Test
    void registerShouldThrowWhenEmailExists() {
        when(userRepository.findByEmail("john@cravio.com")).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> userService.register(new RegisterRequest(
                "john@cravio.com", "password123", "John", Role.CUSTOMER
        )));
    }

    @Test
    void loginShouldThrowForInvalidPassword() {
        when(userRepository.findByEmail("john@cravio.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> userService.login(new LoginRequest("john@cravio.com", "wrong")));
    }

    @Test
    void loginShouldReturnToken() {
        when(userRepository.findByEmail("john@cravio.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(jwtService.generateToken(any(), any())).thenReturn("jwt-token");

        var response = userService.login(new LoginRequest("john@cravio.com", "password"));

        assertEquals("jwt-token", response.token());
        assertEquals("john@cravio.com", response.user().email());
    }
}

