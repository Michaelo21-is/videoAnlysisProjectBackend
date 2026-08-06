package com.moj.purchaseservice.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moj.purchaseservice.Dto.VideoAnalyzerDiagramDto;
import com.moj.purchaseservice.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class VideoAnalyzerDiagramResponse {
    private VideoAnalyzerDiagramDto diagram;

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("diagram_id")
    private String diagramId;

    private String message;

    private OrderStatus status;
}
