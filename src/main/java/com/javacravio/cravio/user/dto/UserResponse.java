package com.javacravio.cravio.user.dto;

import com.javacravio.cravio.user.model.Role;

public record UserResponse(Long id, String email, String fullName, Role role) {
}

