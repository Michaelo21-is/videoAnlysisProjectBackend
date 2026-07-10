package com.moj.authservice.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class SignUpDto {
    private String fullName;
    private String email;
    private String password;
}
