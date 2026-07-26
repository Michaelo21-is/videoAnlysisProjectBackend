package com.moj.purchaseservice.Response;

import com.moj.purchaseservice.enums.ContentType;
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
public class OrderAnalyzeResponse {
    private Long orderId;
    private Long creditCost;
    private SumOfContent sumOfContent;
    private ContentType contentType;
    private UUID userId;
}
