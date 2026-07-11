package com.moj.apigateway.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

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
                // Stateless gateway with bearer tokens: CSRF is not applicable.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // No browser login / basic auth on the gateway.
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                // CORS is handled by Spring Cloud Gateway's globalcors (application.yml);
                // intentionally NOT configured here to avoid duplicate/conflicting CORS handling.
                .authorizeExchange(exchange -> exchange
                        // Let CORS pre-flight requests through to the gateway CORS handler.
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Auth endpoints (login/register) must be reachable without a token.
                        .pathMatchers("/api/auth/sign-up").permitAll()
                        .pathMatchers("/api/auth/sign-in").permitAll()

                        // Everything else requires a valid JWT.
                        .anyExchange().authenticated())
                // Plug the JWT filter into the security chain at the authentication stage.
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
