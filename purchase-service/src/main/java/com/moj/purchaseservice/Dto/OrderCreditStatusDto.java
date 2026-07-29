package com.moj.purchaseservice.Dto;

import com.moj.purchaseservice.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreditStatusDto {
    private Long orderId;
    private Long creditAdded;
    private BigDecimal priceInUsd;
    private String email;
    private String fullName;
    private OrderStatus orderCreditStatus;
}
