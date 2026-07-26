package com.moj.purchaseservice.Utils;

import com.moj.purchaseservice.enums.ContentType;
import com.moj.purchaseservice.enums.SumOfContent;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ContentPricingCalculator {

    public long calculateCreditCost(
            SumOfContent contentAmount,
            ContentType contentType
    ) {
        return switch (contentType) {
            case VIDEO -> switch (contentAmount) {
                case THREE -> 100L;
                case FIVE -> 150L;
                case EIGHT -> 200L;
            };

            case IMAGE -> switch (contentAmount) {
                case THREE -> 50L;
                case FIVE -> 80L;
                case EIGHT -> 100L;
            };

            case TEXT -> switch (contentAmount) {
                case THREE -> 25L;
                case FIVE -> 40L;
                case EIGHT -> 50L;
            };
        };
    }
}