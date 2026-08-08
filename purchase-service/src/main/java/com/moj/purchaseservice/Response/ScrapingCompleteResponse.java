package com.moj.purchaseservice.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moj.purchaseservice.enums.ScrapingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScrapingCompleteResponse {
    @JsonProperty("order_id")
    private Long orderId;
    private String message;
    private ScrapingStatus status;

}
