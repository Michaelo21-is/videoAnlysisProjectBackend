package com.moj.purchaseservice.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class DiagramArrowDto {
    private String id;

    @JsonProperty("sourceNodeId")
    private String sourceNodeId;

    @JsonProperty("targetNodeId")
    private String targetNodeId;

    private String text;
}
