package com.moj.apigateway.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Endpoint the frontend calls once on startup to receive the XSRF-TOKEN cookie.
 * The cookie itself is written by the csrfCookieWebFilter in SecurityConfig; this
 * handler only needs to exist so the request has something to hit (204, no body).
 */
@RestController
public class CsrfController {

    @GetMapping("/api/csrf")
    public Mono<ResponseEntity<Void>> csrf() {
        return Mono.just(ResponseEntity.noContent().build());
    }
}
