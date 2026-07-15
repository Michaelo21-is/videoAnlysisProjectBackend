package com.moj.apigateway.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.csrf.XorServerCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

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
                        // Readable (non-HttpOnly) XSRF-TOKEN cookie so Axios can mirror it
                        // into the X-XSRF-TOKEN header on every mutating request.
                        .csrfTokenRepository(
                                CookieServerCsrfTokenRepository.withHttpOnlyFalse()
                        )
                        .csrfTokenRequestHandler(new SpaServerCsrfTokenRequestHandler())
                )
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/api/csrf").permitAll()
                        .pathMatchers("/api/auth/sign-up").permitAll()
                        .pathMatchers("/api/auth/sign-in").permitAll()
                        .pathMatchers("/api/auth/forgot-my-password-request").permitAll()
                        // The refresh-token cookie itself is the credential here; the request
                        // carries no access token, so it must pass the gateway unauthenticated.
                        // CSRF protection still applies to it like any other POST.
                        .pathMatchers(HttpMethod.POST, "/api/auth/refresh-token").permitAll()

                        // 2FA verification is reachable ONLY with a temp token (TEMP_AUTH),
                        // never with a full access token.
                        .pathMatchers(HttpMethod.POST, "/api/auth/verify-2fa").hasAuthority(JwtAuthenticationFilter.TEMP_AUTHORITY)
                        .pathMatchers(HttpMethod.POST,"/api/auth/renew-2fa").hasAuthority(JwtAuthenticationFilter.TEMP_AUTHORITY)
                        .pathMatchers(HttpMethod.POST,"/api/auth/set-new-password").hasAuthority(JwtAuthenticationFilter.TEMP_AUTHORITY)
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

    /**
     * Since Spring Security 6 the CsrfToken is deferred: the XSRF-TOKEN cookie is only
     * written if something subscribes to the token. This filter subscribes on every
     * request so the cookie is actually sent to the browser.
     */
    @Bean
    public WebFilter csrfCookieWebFilter() {
        return (exchange, chain) -> {
            Mono<CsrfToken> csrfToken = exchange.getAttributeOrDefault(CsrfToken.class.getName(), Mono.empty());
            return csrfToken.then(chain.filter(exchange));
        };
    }

    /**
     * SPA CSRF handler (from the Spring Security reference docs): renders the token
     * XOR-masked (BREACH protection) but accepts the raw token value that Axios copies
     * from the XSRF-TOKEN cookie into the X-XSRF-TOKEN header. With the default handler
     * alone, the raw header value would never validate and every POST would be rejected.
     */
    static final class SpaServerCsrfTokenRequestHandler extends ServerCsrfTokenRequestAttributeHandler {
        private final ServerCsrfTokenRequestAttributeHandler plain = new ServerCsrfTokenRequestAttributeHandler();
        private final ServerCsrfTokenRequestAttributeHandler xor = new XorServerCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(ServerWebExchange exchange, Mono<CsrfToken> csrfToken) {
            this.xor.handle(exchange, csrfToken);
        }

        @Override
        public Mono<String> resolveCsrfTokenValue(ServerWebExchange exchange, CsrfToken csrfToken) {
            String headerValue = exchange.getRequest().getHeaders().getFirst(csrfToken.getHeaderName());
            // Header present -> raw value from the cookie (Axios). Otherwise fall back to
            // the XOR-masked resolution (e.g. token rendered into a form).
            return (StringUtils.hasText(headerValue) ? this.plain : this.xor)
                    .resolveCsrfTokenValue(exchange, csrfToken);
        }
    }
}
