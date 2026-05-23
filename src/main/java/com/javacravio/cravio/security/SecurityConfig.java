package com.javacravio.cravio.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter,
            AuthenticationProvider provider
    ) throws Exception {

        return http
                // 🔴 Disable CSRF for REST APIs
                .csrf(csrf -> csrf.disable())

                // ✅ Enable CORS
                .cors(withDefaults())

                // 🔴 Stateless session (JWT)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 🔴 Handle unauthorized properly
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(unauthorizedEntryPoint())
                )

                // 🔥 AUTHORIZATION RULES
                .authorizeHttpRequests(auth -> auth

                        // ✅ Allow preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 🔥 CRITICAL: Allow auth endpoints (API path)
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // ✅ Swagger + health
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api/v1/docs/**",
                                "/actuator/health"
                        ).permitAll()

                        // ✅ Public restaurant listing
                        .requestMatchers(HttpMethod.GET, "/api/v1/restaurants/**").permitAll()

                        // 🔴 Everything else secured
                        .anyRequest().authenticated()
                )

                // 🔥 Attach JWT filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) ->
                response.sendError(401, "Unauthorized");
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }
}