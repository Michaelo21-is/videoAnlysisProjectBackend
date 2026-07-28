package com.moj.purchaseservice.Service;

import com.moj.purchaseservice.Configuration.PaddleSignatureVerifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class PaddleWebHookService {
    private final ObjectMapper objectMapper;
    private final PurchaseService purchaseService;
    private final PaddleSignatureVerifier paddleSignatureVerifier;

    public PaddleWebHookService(ObjectMapper objectMapper, PurchaseService purchaseService, PaddleSignatureVerifier paddleSignatureVerifier) {
        this.objectMapper = objectMapper;
        this.purchaseService = purchaseService;
        this.paddleSignatureVerifier = paddleSignatureVerifier;
    }
    public void handleWebHook(String rawBody, String paddleSignature) {

    }
}
