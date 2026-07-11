package com.moj.authservice.Service;

import com.moj.authservice.Entity.Jwt;
import com.moj.authservice.Entity.Users;
import com.moj.authservice.Enums.TokenType;
import com.moj.authservice.Repository.JWTRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    private final Long ACCESS_TOKEN_EXPIRATION_TIME_MS = 15L * 60 * 1000;
    private final Long REFRESH_TOKEN_EXPIRATION_TIME_MS = 14L * 24 * 60 * 60 * 1000;
    private final Long TWO_FACTOR_CODE_EXPIRATION_TIME_MS = 10L * 60 * 1000;

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

    private String buildToken(Map<String, Object> extraClaims, String subject, Long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(Users user, TokenType tokenType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        // Token type is required so the API Gateway can cryptographically distinguish an
        // ACCESS token from a short-lived TEMPORARY (2FA) token — otherwise the two are
        // indistinguishable and a temp token could be replayed as a full access token.
        claims.put("type", tokenType.name());

        if (user.getRole() != null) {
            claims.put("role", user.getRole().name());
        }
        Long expirationTime ;
        if (tokenType.equals(TokenType.ACCESS)) {
            expirationTime = ACCESS_TOKEN_EXPIRATION_TIME_MS;
        }
        else if (tokenType.equals(TokenType.REFRESH)){
            expirationTime = REFRESH_TOKEN_EXPIRATION_TIME_MS;
        }
        else{
            expirationTime = TWO_FACTOR_CODE_EXPIRATION_TIME_MS;
        }
        return buildToken(claims, user.getEmail(), expirationTime);
    }

    @Transactional
    public void saveToken(String token, Users user){
        Instant refreshExpiry = Instant.now().plus(14, ChronoUnit.DAYS);
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
    public void renewAccessToken(String refreshToken){
        Jwt jwtToken = jwtRepository.findByRefreshToken(refreshToken)
                .orElse(null);
        if (jwtToken == null || jwtToken.getExpirationDate().isBefore(Instant.now())) {
            return;
        }
        String newAccessToken = generateToken(jwtToken.getUsers(), TokenType.ACCESS);
        String newRefreshToken = generateToken(jwtToken.getUsers(), TokenType.REFRESH);
        saveToken(newRefreshToken, jwtToken.getUsers());

        // need to setup cookie
    }
}
