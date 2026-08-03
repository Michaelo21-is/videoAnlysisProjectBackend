package com.moj.userservice.Response;

import com.moj.userservice.Enums.OrderCreditStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderVideoAnalysisStatusResponse {
    private OrderCreditStatus status;
    private Long orderId;
}
