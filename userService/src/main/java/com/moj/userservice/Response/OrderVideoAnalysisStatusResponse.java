package com.moj.userservice.Response;

import com.moj.userservice.Enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderVideoAnalysisStatusResponse {
    private OrderStatus status;
    private Long orderId;
}
