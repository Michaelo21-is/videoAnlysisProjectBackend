package com.moj.apigateway.Configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    ) {
        return http

                // CORS must be handled by the security chain so it also covers local
                // controllers (e.g. /api/csrf) — the gateway globalcors settings only
                // apply to requests matched by gateway routes.
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

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

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                new HttpStatusServerEntryPoint(
                                        HttpStatus.UNAUTHORIZED
                                )
                        )
                        .accessDeniedHandler(
                                new HttpStatusServerAccessDeniedHandler(
                                        HttpStatus.FORBIDDEN
                                )
                        )
                )


                .authorizeExchange(exchange -> exchange

                        // Allow browser preflight requests
                        .pathMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        // Public endpoints
                        .pathMatchers(
                                "/api/csrf",
                                "/api/auth/sign-up",
                                "/api/auth/sign-in",
                                "/api/auth/forgot-my-password-request",
                                "/api/auth/set-new-password",
                                "/api/auth/password-reset/validate",
                                "/api/auth/refresh-token",
                                "/api/purchase/paddle/webhook"

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
                                "/api/auth/renew-2fa"
                        )
                        .hasAuthority(
                                JwtAuthenticationFilter.TEMP_AUTHORITY
                        )
                        .pathMatchers(HttpMethod.GET, "/api/auth/two-factor/status")
                        .hasAuthority(JwtAuthenticationFilter.TEMP_AUTHORITY)

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

    /**
     * Single CORS definition for everything the gateway serves — both locally
     * handled controllers and proxied routes. Allows the SPA origin to send
     * credentialed requests (cookies) and the X-XSRF-TOKEN header.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${WEBSITE_URL}") String websiteUrl
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(websiteUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}