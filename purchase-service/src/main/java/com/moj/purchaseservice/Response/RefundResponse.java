package com.moj.purchaseservice.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefundResponse {
    private UUID userId;
    private Long credit;
    private Long orderId;
}
