package com.moj.userservice.Response;

import com.moj.userservice.Enums.AnalyzeOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseWithCreditResponse {
    private Long orderId;
    private AnalyzeOrderStatus analyzeOrderStatus;
}
