package com.moj.userservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class BusinessDetailsDto {
    private String businessName;
    private String niche;
    private String description;
    private String targetAudience;
}
