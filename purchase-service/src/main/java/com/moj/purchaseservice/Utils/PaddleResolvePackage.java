package com.moj.purchaseservice.Utils;

import com.moj.purchaseservice.Response.ResolvePackageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaddleResolvePackage {

    private final String starterProductId;
    private final String creatorProductId;
    private final String studioProductId;

    public PaddleResolvePackage(
            @Value("${PADDLE_PRODUCT_ID_STARTER}") String starterProductId,
            @Value("${PADDLE_PRODUCT_ID_CREATOR}") String creatorProductId,
            @Value("${PADDLE_PRODUCT_ID_STUDIO}") String studioProductId
    ) {
        this.starterProductId = starterProductId;
        this.creatorProductId = creatorProductId;
        this.studioProductId = studioProductId;
    }

    public ResolvePackageResponse resolvePriceId(String productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Package ID is required");
        }
        if (productId.equals(starterProductId)) {
            return ResolvePackageResponse.builder()
                    .credit(100L)
                    .priceInUsd(new BigDecimal("10.00"))
                    .build();
        }

        if (productId.equals(creatorProductId)) {
            return ResolvePackageResponse.builder()
                    .credit(600L)
                    .priceInUsd(new BigDecimal("40.00"))
                    .build();
        }

        if (productId.equals(studioProductId)) {
            return ResolvePackageResponse.builder()
                    .credit(2000L)
                    .priceInUsd(new BigDecimal("100.00"))
                    .build();
        }

        throw new IllegalArgumentException("Invalid package ID");
    }
}