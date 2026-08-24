package com.moj.purchaseservice.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ExtractedVideoDetails {
    private String id;
    private String caption;
    private String url;

    private Integer views;
    private Integer likes;
    private Integer comments;
    private Integer shares;

    private String createdAt;
    private String thumbnail;

    @JsonProperty("creator")
    private CreatorDetails creatorDetails;

    // Boxed on purpose: the LLM service sends null whenever the platform does
    // not report a duration, and Jackson refuses to map null onto a primitive.
    private Integer duration;
}
