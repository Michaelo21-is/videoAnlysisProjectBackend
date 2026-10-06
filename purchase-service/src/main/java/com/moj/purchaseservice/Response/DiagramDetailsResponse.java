package com.moj.purchaseservice.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DiagramDetailsResponse {
    private String diagramId;
    private String prompt;
    private String videoName;
}
