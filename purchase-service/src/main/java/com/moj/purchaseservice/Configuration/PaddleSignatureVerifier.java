package com.moj.purchaseservice.Configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Component
public class PaddleSignatureVerifier {

    private static final long TIMESTAMP_TOLERANCE_SECONDS = 5;

    private final String webhookSecret;

    public PaddleSignatureVerifier(
            @Value("${paddle.webhook-secret}") String webhookSecret
    ) {
        this.webhookSecret = webhookSecret;
    }

    public void verify(String rawBody, String paddleSignature) {
        if (paddleSignature == null || paddleSignature.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing Paddle-Signature header"
            );
        }

        Long timestamp = null;
        List<String> signatures = new ArrayList<>();

        for (String part : paddleSignature.split(";")) {
            String[] values = part.trim().split("=", 2);

            if (values.length != 2) {
                continue;
            }

            if ("ts".equals(values[0])) {
                timestamp = Long.valueOf(values[1]);
            }

            if ("h1".equals(values[0])) {
                signatures.add(values[1]);
            }
        }

        if (timestamp == null || signatures.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid Paddle-Signature header"
            );
        }

        validateTimestamp(timestamp);

        String signedPayload = timestamp + ":" + rawBody;
        String expectedSignature = createHmac(signedPayload);

        boolean signatureMatches = signatures.stream()
                .anyMatch(signature ->
                        secureEquals(expectedSignature, signature)
                );

        if (!signatureMatches) {
            throw new IllegalArgumentException(
                    "Invalid Paddle webhook signature"
            );
        }
    }

    private void validateTimestamp(long timestamp) {
        long currentTimestamp = Instant.now().getEpochSecond();
        long difference = Math.abs(currentTimestamp - timestamp);

        if (difference > TIMESTAMP_TOLERANCE_SECONDS) {
            throw new IllegalArgumentException(
                    "Paddle webhook timestamp expired"
            );
        }
    }

    private String createHmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            mac.init(secretKey);

            byte[] signature = mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(signature);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not verify Paddle signature",
                    exception
            );
        }
    }

    private boolean secureEquals(
            String expected,
            String actual
    ) {
        try {
            return MessageDigest.isEqual(
                    HexFormat.of().parseHex(expected),
                    HexFormat.of().parseHex(actual)
            );
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
