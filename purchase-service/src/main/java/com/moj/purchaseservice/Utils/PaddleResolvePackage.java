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

    private final Long starterCredit;
    private final Long creatorCredit;
    private final Long studioCredit;

    private final BigDecimal starterPrice;
    private final BigDecimal creatorPrice;
    private final BigDecimal studioPrice;

    public PaddleResolvePackage(
            @Value("${PADDLE_PRODUCT_ID_STARTER}") String starterProductId,
            @Value("${PADDLE_PRODUCT_ID_CREATOR}") String creatorProductId,
            @Value("${PADDLE_PRODUCT_ID_STUDIO}") String studioProductId,
            @Value("${credit-package.starter}") Long starterCredit,
            @Value("${credit-package.creator}") Long creatorCredit,
            @Value("${credit-package.studio}") Long studioCredit,
            @Value("${pricing-package.starter}") BigDecimal starterPrice,
            @Value("${pricing-package.creator}") BigDecimal creatorPrice,
            @Value("${pricing-package.studio}") BigDecimal studioPrice
    ) {
        this.starterProductId = starterProductId;
        this.creatorProductId = creatorProductId;
        this.studioProductId = studioProductId;
        this.starterCredit = starterCredit;
        this.creatorCredit = creatorCredit;
        this.studioCredit = studioCredit;
        this.starterPrice = starterPrice;
        this.creatorPrice = creatorPrice;
        this.studioPrice = studioPrice;
    }

    public ResolvePackageResponse resolvePriceId(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Package ID is required");
        }

        if (productId.equals(starterProductId)) {
            return createPackageResponse(starterCredit, starterPrice);
        }

        if (productId.equals(creatorProductId)) {
            return createPackageResponse(creatorCredit, creatorPrice);
        }

        if (productId.equals(studioProductId)) {
            return createPackageResponse(studioCredit, studioPrice);
        }

        throw new IllegalArgumentException("Invalid package ID");
    }

    private ResolvePackageResponse createPackageResponse(
            Long credit,
            BigDecimal priceInUsd
    ) {
        return ResolvePackageResponse.builder()
                .credit(credit)
                .priceInUsd(priceInUsd)
                .build();
    }
}