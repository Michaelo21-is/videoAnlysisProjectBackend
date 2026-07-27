package com.moj.notificationservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderCreditDto {
    private String email;
    private String FullName;
    private Long credit;
    private BigDecimal priceInUsd;
    private UUID userId;
}
