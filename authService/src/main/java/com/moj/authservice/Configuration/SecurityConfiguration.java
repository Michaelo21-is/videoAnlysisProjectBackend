package com.moj.authservice.Configuration;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Auth-service security. Mirrors the authorization rules of the API Gateway's
 * {@code SecurityConfig} as a second line of defense: authentication itself is derived
 * from the {@code X-USER-*} headers the gateway sets after validating the JWT
 * (see {@link GatewayHeaderAuthenticationFilter}), never from the token directly.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final GatewayHeaderAuthenticationFilter gatewayHeaderAuthenticationFilter;

    public SecurityConfiguration(GatewayHeaderAuthenticationFilter gatewayHeaderAuthenticationFilter) {
        this.gatewayHeaderAuthenticationFilter = gatewayHeaderAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF protection lives at the gateway (cookie repository + /api/csrf);
                // this service only sees header-authenticated, stateless requests.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Error responses (4xx/5xx) are rendered via a container ERROR
                        // dispatch to /error. Spring Security authorizes that dispatch
                        // too, and /error would fall into anyRequest().hasAnyRole(...),
                        // turning every error on a public endpoint into a bare 403.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/sign-up").permitAll()
                        .requestMatchers("/api/auth/sign-in").permitAll()
                        .requestMatchers("/api/auth/forgot-my-password-request").permitAll()
                        // 2FA verification is reachable ONLY with a temp token (TEMP_AUTH),
                        // never with a full access token — same rule as the gateway.
                        .requestMatchers("/api/auth/set-new-password").hasAuthority(GatewayHeaderAuthenticationFilter.TEMP_AUTHORITY)
                        .requestMatchers(HttpMethod.POST, "/api/auth/verify-2fa").hasAuthority(GatewayHeaderAuthenticationFilter.TEMP_AUTHORITY)
                        .requestMatchers(HttpMethod.POST,"/api/auth/renew-2fa").hasAuthority(GatewayHeaderAuthenticationFilter.TEMP_AUTHORITY)
                        .requestMatchers("/api/auth/two-factor/status").hasAuthority(GatewayHeaderAuthenticationFilter.TEMP_AUTHORITY)
                        .requestMatchers("/error").permitAll()
                        // Every other endpoint requires a real access token, i.e. a normal
                        // role. A temp token only carries TEMP_AUTH, so it is rejected here.
                        .anyRequest().hasAnyRole("USER", "ADMIN")
                )
                .addFilterBefore(gatewayHeaderAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
