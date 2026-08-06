package com.moj.purchaseservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoAnalyzerDiagramDto {
    private String name;
    private String prompt;
    private boolean privateDiagram;
    private List<DiagramNodeDto> nodes;
    private List<DiagramArrowDto> arrows;
}
