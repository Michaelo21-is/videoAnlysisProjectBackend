package com.moj.purchaseservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreatorDetails {
    private String name;
    private String profileUrl;
    private String avatar;
    private Integer followers;
    private Boolean isVerified;
}
