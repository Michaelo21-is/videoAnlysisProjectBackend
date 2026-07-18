package com.moj.apigateway.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class CsrfController {

    @GetMapping("/api/csrf")
    public Mono<ResponseEntity<Void>> csrf(ServerWebExchange exchange) {
        Mono<CsrfToken> csrfToken =
                exchange.getAttribute(CsrfToken.class.getName());

        if (csrfToken == null) {
            return Mono.just(
                    ResponseEntity.noContent().build()
            );
        }

        return csrfToken.thenReturn(
                ResponseEntity.noContent().build()
        );
    }
}