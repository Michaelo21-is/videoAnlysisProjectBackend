package com.moj.purchaseservice.Entity;

import com.moj.purchaseservice.enums.VideoDiagramType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "diagram_details_for_analyze_content")
public class DiagramDetailsForAnalyzeContent {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "diagram_details_for_analyze_content_id_generator"
    )
    @SequenceGenerator(
            name = "diagram_details_for_analyze_content_id_generator",
            sequenceName = "diagram_details_for_analyze_content_id_seq",
            allocationSize = 1
    )
    private Long id;

    @Column(name = "diagram_id", nullable = false)
    private String diagramId;

    @Column(name = "video_name", nullable = false)
    private String videoName;

    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    @Enumerated(EnumType.STRING)
    @Column(name = "diagram_type", nullable = false)
    private VideoDiagramType videoDiagramType;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

}
