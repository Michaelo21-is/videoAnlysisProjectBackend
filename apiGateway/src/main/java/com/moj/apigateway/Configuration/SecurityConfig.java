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
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

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
        ServerWebExchangeMatcher paddleWebhookMatcher =
                ServerWebExchangeMatchers.pathMatchers(
                        HttpMethod.POST,
                        "/api/purchase/paddle/webhook"
                );

        /*
         * Keep Spring's normal CSRF behavior for unsafe HTTP methods,
         * but exclude Paddle's server-to-server webhook.
         */
        ServerWebExchangeMatcher csrfProtectionMatcher =
                new AndServerWebExchangeMatcher(
                        CsrfWebFilter.DEFAULT_CSRF_MATCHER,
                        new NegatedServerWebExchangeMatcher(
                                paddleWebhookMatcher
                        )
                );

        return http
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource)
                )

                .csrf(csrf -> csrf
                        .requireCsrfProtectionMatcher(
                                csrfProtectionMatcher
                        )
                        .csrfTokenRepository(
                                CookieServerCsrfTokenRepository
                                        .withHttpOnlyFalse()
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

                        .pathMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        /*
                         * Paddle authenticates this request using
                         * the Paddle-Signature header.
                         */
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/purchase/paddle/webhook"
                        )
                        .permitAll()

                        .pathMatchers(
                                "/api/csrf",
                                "/api/auth/sign-up",
                                "/api/auth/sign-in",
                                "/api/auth/forgot-my-password-request",
                                "/api/auth/set-new-password",
                                "/api/auth/password-reset/validate"
                        )
                        .permitAll()

                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/auth/refresh-token"
                        )
                        .permitAll()

                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/auth/verify-2fa",
                                "/api/auth/renew-2fa"
                        )
                        .hasAuthority(
                                JwtAuthenticationFilter.TEMP_AUTHORITY
                        )

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/auth/two-factor/status"
                        )
                        .hasAuthority(
                                JwtAuthenticationFilter.TEMP_AUTHORITY
                        )

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
            @Value("${WEBSITE_URL}") String websiteUrl) {
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