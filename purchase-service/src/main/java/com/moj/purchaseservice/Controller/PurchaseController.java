package com.moj.purchaseservice.Controller;

import com.moj.purchaseservice.Service.PurchaseService;
import com.moj.purchaseservice.enums.ContentType;
import com.moj.purchaseservice.enums.SumOfContent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/purchase")
public class PurchaseController {
    private final PurchaseService purchaseService;
    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }
    @RequestMapping("/analyze-content-request")
    public ResponseEntity<?> analyzeVideos(@RequestHeader("X-USER-ID") UUID userId
            , @RequestParam("sumVideos") SumOfContent sumOfContent, @RequestParam("contentType") ContentType contentType) {
        purchaseService.orderAnalyzeContent(userId, sumOfContent, contentType);
        return ResponseEntity.ok().build();
    }
}
