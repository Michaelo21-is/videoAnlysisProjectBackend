package com.moj.authservice.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccessAndRefreshResponse {
    private String accessToken;
    private String refreshToken;
    private HttpStatus status;
    private String message;
}
