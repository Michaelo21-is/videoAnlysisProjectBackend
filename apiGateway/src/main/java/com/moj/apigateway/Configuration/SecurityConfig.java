package com.moj.apigateway.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http
    ) {
        return http

                // CSRF protection for cookie-based authentication
                .csrf(csrf -> csrf
                        .csrfTokenRepository(
                                CookieServerCsrfTokenRepository.withHttpOnlyFalse()
                        )
                        .csrfTokenRequestHandler(
                                new ServerCsrfTokenRequestAttributeHandler()
                        )
                )

                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                .authorizeExchange(exchange -> exchange

                        // Allow browser preflight requests
                        .pathMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        // Public endpoints
                        .pathMatchers(
                                "/api/csrf",
                                "/api/auth/sign-up",
                                "/api/auth/sign-in",
                                "/api/auth/forgot-my-password-request"
                        )
                        .permitAll()

                        // Refresh token is read from the HttpOnly cookie
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/auth/refresh-token"
                        )
                        .permitAll()

                        // Endpoints that require a temporary token
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/auth/verify-2fa",
                                "/api/auth/renew-2fa",
                                "/api/auth/set-new-password"
                        )
                        .hasAuthority(
                                JwtAuthenticationFilter.TEMP_AUTHORITY
                        )

                        // All other endpoints require a normal authenticated user
                        .anyExchange()
                        .hasAnyRole("USER", "ADMIN")
                )

                .addFilterAt(
                        jwtAuthenticationFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION
                )

                .build();
    }
}