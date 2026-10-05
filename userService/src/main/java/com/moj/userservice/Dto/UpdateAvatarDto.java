package com.moj.userservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAvatarDto {
    private String avatarName;
    private String avatarDescription;
    private Long id;
}
