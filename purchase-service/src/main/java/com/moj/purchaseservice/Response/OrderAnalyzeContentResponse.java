package com.moj.purchaseservice.Response;

import com.moj.purchaseservice.enums.Platform;
import com.moj.purchaseservice.enums.SumOfContent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderAnalyzeContentResponse {
    private Long orderId;
    private Long creditCost;
    private SumOfContent sumOfContent;
    private Platform platform;
    private UUID userId;
}
