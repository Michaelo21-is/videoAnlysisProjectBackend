package com.moj.purchaseservice.Utils;

import com.moj.purchaseservice.enums.SumOfContent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ContentPricingCalculator {

    private final Long threeVideoCreditPrice;
    private final Long fiveVideoCreditPrice;
    private final Long sevenVideoCreditPrice;

    public ContentPricingCalculator(
            @Value("${analyze-content.3-videos}") Long threeVideoCreditPrice,
            @Value("${analyze-content.5-videos}") Long fiveVideoCreditPrice,
            @Value("${analyze-content.7-videos}") Long sevenVideoCreditPrice
    ) {
        this.threeVideoCreditPrice = threeVideoCreditPrice;
        this.fiveVideoCreditPrice = fiveVideoCreditPrice;
        this.sevenVideoCreditPrice = sevenVideoCreditPrice;
    }

    public Long calculatePricingByNumOfContents(SumOfContent sumOfContent) {
        if (sumOfContent == null) {
            throw new IllegalArgumentException("Sum of content cannot be null");
        }

        return switch (sumOfContent) {
            case THREE -> threeVideoCreditPrice;
            case FIVE -> fiveVideoCreditPrice;
            case SEVEN-> sevenVideoCreditPrice;
        };
    }
}