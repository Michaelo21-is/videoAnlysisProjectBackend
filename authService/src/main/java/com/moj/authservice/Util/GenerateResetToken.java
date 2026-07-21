package com.moj.authservice.Util;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@UtilityClass
public class GenerateResetToken {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateResetToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }
    public byte[] hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
