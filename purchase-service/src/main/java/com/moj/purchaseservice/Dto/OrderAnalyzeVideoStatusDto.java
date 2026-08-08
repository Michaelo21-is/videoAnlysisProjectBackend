package com.moj.purchaseservice.Dto;

import com.moj.purchaseservice.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderAnalyzeVideoStatusDto {
    private Long orderId;
    private OrderStatus status;
}
