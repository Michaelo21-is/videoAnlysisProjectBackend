package com.moj.purchaseservice.Controller;

import com.moj.purchaseservice.Service.PurchaseService;
import com.moj.purchaseservice.Service.SseService;
import com.moj.purchaseservice.enums.ContentType;
import com.moj.purchaseservice.enums.SumOfContent;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/purchase")
public class PurchaseController {
    private final PurchaseService purchaseService;
    private final SseService sseService;
    public PurchaseController(PurchaseService purchaseService, SseService sseService) {
        this.purchaseService = purchaseService;
        this.sseService = sseService;
    }
    @PostMapping("/analyze-content-request")
    public ResponseEntity<?> analyzeContent(@RequestHeader("X-USER-ID") UUID userId
            , @RequestParam("amount") SumOfContent sumOfContent, @RequestParam("contentType") ContentType contentType) {
        Long orderId = purchaseService.orderAnalyzeContent(userId, sumOfContent, contentType);
        return ResponseEntity.accepted().body(orderId);
    }
    @GetMapping(value = "/analyze-content/status/{orderId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getAnalyzeContentStatus(@PathVariable("orderId") Long orderId, @RequestHeader("X-USER-ID") UUID userId) {
        return sseService.OrderAnalyzeStatusSSE(orderId, userId);
    }
    @PostMapping("/order-credit-request")
    public ResponseEntity<?> orderCredit(@RequestHeader("X-USER-ID") UUID userId, @RequestBody com.moj.purchaseservice.Dto.OrderCreditDto orderCredit) {
        Long orderId = purchaseService.orderCredit(userId, orderCredit);
        return ResponseEntity.accepted().body(orderId);
    }
}
