package com.moj.notificationservice.Util;

import com.moj.notificationservice.Enums.TwoFactorType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GetMessageByTwoFactorType {
    private static final int EXPIRATION_MINUTES = 15;

    public String getMessage(
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
}
