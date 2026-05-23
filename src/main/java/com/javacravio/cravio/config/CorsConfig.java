package com.javacravio.cravio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cravio.cors.allowed-origins:http://localhost:3000,https://cravio.tech}") String allowedOrigins) {
        List<String> allowedOriginList = allowedOrigins == null
                ? List.of("http://localhost:3000")
                : allowedOrigins
                .lines()
                .flatMap(line -> java.util.Arrays.stream(line.split(",")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOriginList.isEmpty() ? List.of("http://localhost:3000") : allowedOriginList);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
