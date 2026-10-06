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

    @JsonProperty("diagram_name")
    private String diagramName;

    private String message;

    private OrderStatus status;

    @JsonProperty("video_gemini_url")
    private String videoGeminiUrl;

    // read from the LLM service message only, never sent back to the client over SSE
    @JsonProperty(value = "prompt", access = JsonProperty.Access.WRITE_ONLY)
    private String prompt;
}
