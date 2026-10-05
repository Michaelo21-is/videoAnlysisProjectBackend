package com.moj.userservice.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AvatarResponse {
    private Long id;
    private String avatarName;
    private String avatarDescription;
    private String s3Url;
}
