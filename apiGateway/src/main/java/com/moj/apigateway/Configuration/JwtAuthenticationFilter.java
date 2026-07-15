package com.moj.apigateway.Configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpCookie;
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

/**
 * Reactive (WebFlux) gateway authentication filter.
 *
 * <p>Reads the JWT from cookies and validates it locally with the same HS256 secret that
 * auth-service signs tokens with. The token subject is the user id (see auth-service
 * {@code JwtService}). Two token types are supported, distinguished by the
 * {@code type} claim:</p>
 * <ul>
 *     <li><b>access-token</b> cookie, {@code type=ACCESS} — full access, granted {@code ROLE_<role>}.</li>
 *     <li><b>temp-token</b> cookie, {@code type=TEMPORARY} — 2FA step only, granted {@link #TEMP_AUTHORITY}
 *     and deliberately NOT the user's role.</li>
 * </ul>
 *
 * <p>The {@code type} claim is checked against the cookie the token arrived in, so a temp
 * token cannot be replayed in the access cookie (or vice-versa) to escalate privileges.</p>
 */
@Component
public class JwtAuthenticationFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    // Must match the cookie names auth-service sets (see auth-service CookieUtil).
    private static final String ACCESS_TOKEN_COOKIE = "access-token";
    private static final String TEMP_TOKEN_COOKIE = "temp-token";

    private static final String TYPE_CLAIM = "type";
    private static final String TYPE_ACCESS = "ACCESS";
    private static final String TYPE_TEMPORARY = "TEMPORARY";

    private static final String USER_ID_HEADER = "X-USER-ID";
    private static final String USER_ROLE_HEADER = "X-USER-ROLE";

    /** Authority carried by a 2FA temp token; grants access to the 2FA verification endpoint only. */
    static final String TEMP_AUTHORITY = "TEMP_AUTH";

    private final SecretKey signingKey;

    public JwtAuthenticationFilter(@Value("${jwt.secret-key}") String secretKey) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Never trust internal identity headers coming from the client — strip them up front
        // so only this filter can populate them after a successful validation.
        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                })
                .build();

        ServerWebExchange sanitizedExchange = exchange.mutate()
                .request(sanitizedRequest)
                .build();

        // Prefer a full access token when present; otherwise fall back to a 2FA temp token.
        HttpCookie accessCookie = sanitizedRequest.getCookies().getFirst(ACCESS_TOKEN_COOKIE);
        if (accessCookie != null && !accessCookie.getValue().isBlank()) {
            return authenticateAccessToken(sanitizedExchange, chain, accessCookie.getValue());
        }

        HttpCookie tempCookie = sanitizedRequest.getCookies().getFirst(TEMP_TOKEN_COOKIE);
        if (tempCookie != null && !tempCookie.getValue().isBlank()) {
            return authenticateTempToken(sanitizedExchange, chain, tempCookie.getValue());
        }

        // No token: continue unauthenticated and let SecurityConfig's authorization rules decide.
        return chain.filter(sanitizedExchange);
    }

    private Mono<Void> authenticateAccessToken(ServerWebExchange exchange, WebFilterChain chain, String token) {
        final Claims claims;
        try {
            claims = parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Rejected request with invalid access token: {}", e.getMessage());
            return unauthorized(exchange);
        }

        if (!TYPE_ACCESS.equals(claims.get(TYPE_CLAIM, String.class))) {
            log.warn("Rejected non-ACCESS token presented in the accessToken cookie");
            return unauthorized(exchange);
        }

        String userId = claims.getSubject();
        String role = claims.get("role", String.class);

        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
        if (userId != null) {
            requestBuilder.header(USER_ID_HEADER, userId);
        }
        if (role != null) {
            requestBuilder.header(USER_ROLE_HEADER, role);
        }

        List<SimpleGrantedAuthority> authorities = role == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + role));

        return authenticate(exchange, chain, requestBuilder.build(), userId, authorities);
    }

    private Mono<Void> authenticateTempToken(ServerWebExchange exchange, WebFilterChain chain, String token) {
        final Claims claims;
        try {
            claims = parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Rejected request with invalid temp token: {}", e.getMessage());
            return unauthorized(exchange);
        }

        if (!TYPE_TEMPORARY.equals(claims.get(TYPE_CLAIM, String.class))) {
            log.warn("Rejected non-TEMPORARY token presented in the tempToken cookie");
            return unauthorized(exchange);
        }

        String userId = claims.getSubject();

        // Forward only the identity a 2FA step needs — deliberately NOT the user's role.
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
        if (userId != null) {
            requestBuilder.header(USER_ID_HEADER, userId);
        }

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(TEMP_AUTHORITY));

        return authenticate(exchange, chain, requestBuilder.build(), userId, authorities);
    }

    private Mono<Void> authenticate(ServerWebExchange exchange,
                                    WebFilterChain chain,
                                    ServerHttpRequest mutatedRequest,
                                    String principal,
                                    List<SimpleGrantedAuthority> authorities) {
        ServerWebExchange authenticatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);

        return chain.filter(authenticatedExchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private Claims parse(String token) {
        // parseSignedClaims verifies the signature and enforces expiration (throws on both).
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
