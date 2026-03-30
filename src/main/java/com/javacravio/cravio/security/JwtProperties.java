package com.javacravio.cravio.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cravio.jwt")
public record JwtProperties(String secret, long expirationMillis) {
}

