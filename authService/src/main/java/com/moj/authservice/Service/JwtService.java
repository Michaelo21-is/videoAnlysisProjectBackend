package com.moj.authservice.Service;

import com.moj.authservice.Entity.Jwt;
import com.moj.authservice.Entity.Users;
import com.moj.authservice.Enums.TokenType;
import com.moj.authservice.Repository.JWTRepository;
import com.moj.authservice.Response.AccessAndRefreshResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    public static final Duration ACCESS_TOKEN_EXPIRATION =
            Duration.ofMinutes(15);

    public static final Duration REFRESH_TOKEN_EXPIRATION =
            Duration.ofDays(14);

    public static final Duration TEMP_TOKEN_EXPIRATION =
            Duration.ofMinutes(10);


    private final JWTRepository jwtRepository;
    private final String secretKey;
    public JwtService(JWTRepository jwtRepository, @Value("${jwt.secret-key}") String secretKey) {
        this.jwtRepository = jwtRepository;
        this.secretKey = secretKey;
    }


    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            String subject,
            Duration expiration
    ) {
        Instant now = Instant.now();
        Instant expirationTime = now.plus(expiration);

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expirationTime))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(Users user, TokenType tokenType) {
        Map<String, Object> claims = new HashMap<>();
        // Token type is required so the API Gateway can cryptographically distinguish an
        // ACCESS token from a short-lived TEMPORARY (2FA) token — otherwise the two are
        // indistinguishable and a temp token could be replayed as a full access token.
        claims.put("type", tokenType.name());

        if (user.getRole() != null) {
            claims.put("role", user.getRole().name());
        }
        Duration expiration = switch (tokenType) {
            case ACCESS -> ACCESS_TOKEN_EXPIRATION;
            case REFRESH -> REFRESH_TOKEN_EXPIRATION;
            case TEMPORARY -> TEMP_TOKEN_EXPIRATION;
        };
        // The user id is the token subject — the gateway forwards it as X-USER-ID.
        return buildToken(claims, user.getId().toString(), expiration);
    }

    @Transactional
    public void saveToken(String token, Users user){
        Instant refreshExpiry =
                Instant.now().plus(REFRESH_TOKEN_EXPIRATION);
        Jwt jwtToken = jwtRepository.findByUsersId(user.getId())
                .orElse(
                        Jwt.builder()
                                .users(user)
                                .build()
                );
        jwtToken.setRefreshToken(token);
        jwtToken.setExpirationDate(refreshExpiry);

        jwtRepository.save(jwtToken);
    }





    public void deleteToken(UUID userId){
        jwtRepository.deleteAllByUsersId(userId);
    }
    @Transactional
    public AccessAndRefreshResponse renewAccessToken(String refreshToken){
        Jwt jwtToken = jwtRepository.findByRefreshToken(refreshToken)
                .orElse(null);
        if (jwtToken == null || jwtToken.getExpirationDate().isBefore(Instant.now())) {
            return AccessAndRefreshResponse.builder()
                    .message("refresh token is invalid or expired")
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }
        String newAccessToken = generateToken(jwtToken.getUsers(), TokenType.ACCESS);
        String newRefreshToken = generateToken(jwtToken.getUsers(), TokenType.REFRESH);
        saveToken(newRefreshToken, jwtToken.getUsers());

        return AccessAndRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .message("access token renewed successfully")
                .status(HttpStatus.OK)
                .build();
    }
}
