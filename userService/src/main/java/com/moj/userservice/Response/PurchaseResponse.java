package com.moj.userservice.Response;

import com.moj.userservice.Enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseResponse {
    private Long orderId;
    private Status status;
}
