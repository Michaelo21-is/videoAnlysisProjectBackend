package com.moj.purchaseservice.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moj.purchaseservice.enums.DiagramNodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class DiagramNodeDto {
    private String id;

    private DiagramNodeType type;

    private String text;

    private Double x;

    private Double y;

    private Double width;

    private Double height;

    @JsonProperty("zIndex")
    private Integer zIndex;
}
