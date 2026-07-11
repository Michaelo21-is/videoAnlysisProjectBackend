package com.moj.apigateway.Configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;


@Component
public class JwtAuthenticationFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-USER-ID";
    private static final String USER_EMAIL_HEADER = "X-USER-EMAIL";
    private static final String USER_ROLE_HEADER = "X-USER-ROLE";

    private final SecretKey signingKey;

    public JwtAuthenticationFilter(@Value("${jwt.secret-key}") String secretKey) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Never trust identity headers coming from the client — strip them up front so only
        // this filter can populate them after a successful validation.
        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_EMAIL_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                })
                .build();
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();

        String authHeader = sanitizedRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // No bearer token: continue unauthenticated and let SecurityConfig's authorization
        // rules decide (public routes pass, protected routes get 401).
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(sanitizedExchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        final Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            // A token was presented but is invalid/expired/tampered -> reject explicitly.
            log.warn("Rejected request with invalid JWT: {}", e.getMessage());
            return unauthorized(sanitizedExchange);
        }

        String email = claims.getSubject();
        String userId = claims.get("userId") != null ? claims.get("userId").toString() : null;
        String role = claims.get("role") != null ? claims.get("role").toString() : null;

        // Forward the resolved identity to downstream services as trusted headers.
        ServerHttpRequest.Builder mutated = sanitizedExchange.getRequest().mutate();
        if (userId != null) {
            mutated.header(USER_ID_HEADER, userId);
        }
        if (email != null) {
            mutated.header(USER_EMAIL_HEADER, email);
        }
        if (role != null) {
            mutated.header(USER_ROLE_HEADER, role);
        }
        ServerWebExchange authenticatedExchange = sanitizedExchange.mutate()
                .request(mutated.build())
                .build();

        List<SimpleGrantedAuthority> authorities = role != null
                ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                : List.of();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(email, null, authorities);

        return chain.filter(authenticatedExchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
