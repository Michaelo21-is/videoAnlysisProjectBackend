package com.moj.purchaseservice.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moj.purchaseservice.Dto.VideoAnalyzerDiagramDto;
import com.moj.purchaseservice.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class VideoAnalyzerDiagramResponse {
    private VideoAnalyzerDiagramDto diagram;

    @JsonProperty("order_id")
    private Long orderId;

    private String message;

    private OrderStatus status;
}
