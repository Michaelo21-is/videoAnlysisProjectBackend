package com.moj.purchaseservice.Dto;

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

    private String created_at;
    private String thumbnail;

    private CreatorDetails creatorDetails;

    private int duration;
}
