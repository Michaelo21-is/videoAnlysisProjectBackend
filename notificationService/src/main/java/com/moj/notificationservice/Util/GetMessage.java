package com.moj.notificationservice.Util;

import com.moj.notificationservice.Enums.TwoFactorType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class GetMessage {
    private static final int EXPIRATION_MINUTES = 15;
    private final String websiteUrl;
    public GetMessage(
            @Value("${WEBSITE_URL}") String websiteUrl
    ) {
        this.websiteUrl = websiteUrl;
    }
    public String getMessageByTwoFactor(
            TwoFactorType twoFactorType,
            Integer verificationCode
    ) {
        if (twoFactorType == TwoFactorType.PASSWORDRESET) {
            return "We received a request to reset your password. "
                    + "Your password reset code is: "
                    + verificationCode
                    + ". This code will expire in "
                    + EXPIRATION_MINUTES
                    + " minutes.";
        }

        if (twoFactorType == TwoFactorType.EMAILVERIFICATION) {
            return "Thank you for signing up. "
                    + "Your email verification code is: "
                    + verificationCode
                    + ". This code will expire in "
                    + EXPIRATION_MINUTES
                    + " minutes.";
        }

        throw new IllegalArgumentException(
                "Unsupported two-factor type: " + twoFactorType
        );
    }
    public String getMessageByPasswordReset(String resetToken) {
        String resetLink =
                websiteUrl
                        + "/set-password/"
                        + resetToken;

        return "We received a request to reset your password. "
                + "Click the following link to reset it: "
                + resetLink
                + ". This link will expire in "
                + EXPIRATION_MINUTES
                + " minutes.";
    }
    public String getMessageByOrderCredit(String fullName, Long credits, BigDecimal priceInUsd) {
        return String.format(
                "Hi %s, your order was placed successfully. You purchased %d credits for $%s.",
                fullName,
                credits,
                priceInUsd.toPlainString()
        );
    }
}
