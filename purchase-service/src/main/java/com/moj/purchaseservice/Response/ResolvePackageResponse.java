package com.moj.purchaseservice.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ResolvePackageResponse {
    private BigDecimal priceInUsd;
    private Long credit;
}
