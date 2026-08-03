package com.moj.purchaseservice.Controller;

import com.moj.purchaseservice.Dto.OrderCreditDto;
import com.moj.purchaseservice.Service.*;
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
    private final SseOrderAnalyzeContentService sseOrderAnalyzeContentService;
    private final SseOrderCreditService sseOrderCreditService;
    private final SseOrderAnalyzeVideoService sseOrderAnalyzeVideoService;
    private final PaddleWebHookService paddleWebHookService;
    public PurchaseController(PurchaseService purchaseService, SseOrderAnalyzeContentService sseOrderAnalyzeContentService
    , PaddleWebHookService paddleWebHookService, SseOrderCreditService sseOrderCreditService ,
      SseOrderAnalyzeVideoService sseOrderAnalyzeVideoService) {
        this.purchaseService = purchaseService;
        this.sseOrderAnalyzeContentService = sseOrderAnalyzeContentService;
        this.paddleWebHookService = paddleWebHookService;
        this.sseOrderCreditService = sseOrderCreditService;
        this.sseOrderAnalyzeVideoService = sseOrderAnalyzeVideoService;
    }

    @PostMapping("/order-credit-request")
    public ResponseEntity<?> orderCredit(@RequestHeader("X-USER-ID") UUID userId, @RequestBody OrderCreditDto orderCredit) {
        Long orderId = purchaseService.orderCredit(userId, orderCredit);
        return ResponseEntity.accepted().body(orderId);
    }

    @PostMapping(value = "/paddle/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> paddleWebhook(@RequestBody String rawBody,
    @RequestHeader("Paddle-Signature") String paddleSignature ) {
        paddleWebHookService.handleWebHook(rawBody, paddleSignature);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/order-credit/{orderId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getOrderCreditStatus(@PathVariable("orderId") Long orderId, @RequestHeader("X-USER-ID") UUID userId) {
        return sseOrderCreditService.OrderAnalyzeStatusSSE(orderId, userId);
    }

    @PostMapping("/analyze-content-request")
    public ResponseEntity<?> analyzeContent(@RequestHeader("X-USER-ID") UUID userId
            , @RequestParam("amount") SumOfContent sumOfContent, @RequestParam("contentType") ContentType contentType) {
        Long orderId = purchaseService.orderAnalyzeContent(userId, sumOfContent, contentType);
        return ResponseEntity.accepted().body(orderId);
    }
    @GetMapping(value = "/analyze-content/status/{orderId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getAnalyzeContentStatus(@PathVariable("orderId") Long orderId, @RequestHeader("X-USER-ID") UUID userId) {
        return sseOrderAnalyzeContentService.OrderAnalyzeStatusSSE(orderId, userId);
    }
    @PostMapping("/order-analyze-video")
    public ResponseEntity<?> analyzeVideo(@RequestHeader("X-USER-ID") UUID userId) {
        Long orderId = purchaseService.orderAnalyzeVideo(userId);
        return ResponseEntity.accepted().body(orderId);
    }
    @GetMapping(value = "/order-analyze-video/status/{orderId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getAnalyzeVideoStatus(@PathVariable("orderId") Long orderId, @RequestHeader("X-USER-ID") UUID userId) {
        return sseOrderAnalyzeVideoService.OrderAnalyzeStatusSSE(orderId, userId);
    }

}
