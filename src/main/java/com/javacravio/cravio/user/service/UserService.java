package com.javacravio.cravio.user.service;

import com.javacravio.cravio.user.dto.AuthResponse;
import com.javacravio.cravio.user.dto.LoginRequest;
import com.javacravio.cravio.user.dto.RegisterRequest;
import com.javacravio.cravio.user.dto.UserResponse;

public interface UserService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}

