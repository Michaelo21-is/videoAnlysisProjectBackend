package com.moj.apigateway.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(
                                CookieServerCsrfTokenRepository.withHttpOnlyFalse()
                        )
                )
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/api/csrf").permitAll()
                        .pathMatchers("/api/auth/sign-up").permitAll()
                        .pathMatchers("/api/auth/sign-in").permitAll()
                        // 2FA verification is reachable ONLY with a temp token (TEMP_AUTH),
                        // never with a full access token.
                        .pathMatchers(HttpMethod.POST, "/api/auth/verify-2fa")
                        .hasAuthority(JwtAuthenticationFilter.TEMP_AUTHORITY)
                        // Every other protected endpoint requires a real access token, i.e. a
                        // normal role. A temp token only carries TEMP_AUTH, so it is rejected here.
                        .anyExchange().hasAnyRole("USER", "ADMIN")
                )
                .addFilterAt(
                        jwtAuthenticationFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION
                )
                .build();
    }
}
