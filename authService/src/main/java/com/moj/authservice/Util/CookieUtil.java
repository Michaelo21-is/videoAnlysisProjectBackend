package com.moj.authservice.Util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

// has to use component to load variable from .env file
@Component
public class CookieUtil {
    public static final String tempToken = "temp-token";
    public static final String refreshToken = "refresh-token";
    public static final String accessToken = "access-token";

    // The refresh token is the longest-lived credential, so its cookie is scoped to the
    // one endpoint that consumes it instead of being sent with every request.
    public static final String refreshTokenPath = "/api/auth/refresh-token";


    private final boolean secure;
    private final String sameSite;

    public CookieUtil(@Value("${app.cookie.secure:false}") boolean secure, @Value("${app.cookie.same-site:Lax}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public ResponseCookie createJwtCookie(String name, String token, Duration expiration) {
        return createJwtCookie(name, token, expiration, "/");
    }

    public ResponseCookie createJwtCookie(String name, String token, Duration expiration, String path) {
        return ResponseCookie.from(name, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(expiration)
                .build();
    }

    public ResponseCookie deleteCookie(String name) {
        return deleteCookie(name, "/");
    }

    // Deletion only works when the path matches the one the cookie was created with.
    public ResponseCookie deleteCookie(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(Duration.ZERO)
                .build();
    }
}