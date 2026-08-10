package com.moj.purchaseservice.Response;

import com.moj.purchaseservice.enums.OrderAnalyzeVideoStatusType;
import com.moj.purchaseservice.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderAnalyzeVideoStatusResponse {
    private Status status;
    private String message;
    private Long orderId;
    private OrderAnalyzeVideoStatusType orderAnalyzeVideoStatusType;
}
