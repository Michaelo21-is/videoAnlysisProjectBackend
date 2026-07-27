package com.moj.purchaseservice.Dto;

import com.moj.purchaseservice.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusDto {

    private Long orderId;
    private Status status;

}
