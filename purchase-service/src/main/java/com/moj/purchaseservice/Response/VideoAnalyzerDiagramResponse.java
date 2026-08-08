package com.moj.purchaseservice.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("order_id")
    private Long orderId;

    @JsonProperty("diagram_id")
    private String diagramId;

    private String message;

    private OrderStatus status;

    @JsonProperty("video_gemini_url")
    private String videoGeminiUrl;
}
