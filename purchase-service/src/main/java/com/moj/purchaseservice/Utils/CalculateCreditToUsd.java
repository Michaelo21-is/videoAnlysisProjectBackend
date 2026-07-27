package com.moj.purchaseservice.Utils;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;

@UtilityClass
public class CalculateCreditToUsd {

    private static final long STUDIO_CREDITS = 2000;
    private static final long CREATOR_CREDITS = 500;

    // Number of credits received for one dollar
    private static final BigDecimal STARTER_PRICE_RATE = new BigDecimal("10");

    private static final BigDecimal CREATOR_PRICE_RATE = new BigDecimal("12.5");

    private static final BigDecimal STUDIO_PRICE_RATE = new BigDecimal("20");

    public BigDecimal calculateCreditToUsd(long credits) {
        if (credits <= 0) {
            throw new IllegalArgumentException(
                    "Credits must be greater than zero"
            );
        }

        BigDecimal priceRate;

        if (credits < CREATOR_CREDITS) {
            priceRate = STARTER_PRICE_RATE;

        } else if (credits < STUDIO_CREDITS) {
            priceRate = CREATOR_PRICE_RATE;

        } else {
            priceRate = STUDIO_PRICE_RATE;
        }

        return BigDecimal.valueOf(credits)
                .divide(priceRate, 2, RoundingMode.HALF_UP);
    }
}