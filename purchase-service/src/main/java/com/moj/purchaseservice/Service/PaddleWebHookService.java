package com.moj.purchaseservice.Service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.moj.purchaseservice.Configuration.PaddleSignatureVerifier;
import org.springframework.stereotype.Service;

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
        // חובה לפני עדכון הזמנה
        paddleSignatureVerifier.verify(
                rawBody,
                paddleSignature
        );

        try {
            JsonNode root = objectMapper.readTree(rawBody);

            String eventType = root
                    .path("event_type")
                    .asText();

            JsonNode customData = root
                    .path("data")
                    .path("custom_data");

            String orderType = customData
                    .path("orderType")
                    .asText();

            String orderIdValue = customData
                    .path("orderId")
                    .asText();

            if (orderIdValue.isBlank()) {
                throw new IllegalArgumentException(
                        "Missing orderId in Paddle custom_data"
                );
            }

            // זה ה-Long של ההזמנה שלך
            Long orderId = Long.valueOf(orderIdValue);

            // מתעלמים מעסקאות שאינן רכישת קרדיטים
            if ("ORDER_CREDIT".equals(orderType)) {
                switch (eventType) {
                    case "transaction.completed" -> purchaseService.orderCreditPurchasedSuccessfully(orderId);


                    case "transaction.payment_failed" -> purchaseService.orderCreditPurchasedFailed(orderId);


                    default -> {
                        // אירוע שלא מעניין אותנו
                    }
                }
            }



        } catch (JacksonException  exception) {
            throw new IllegalArgumentException(
                    "Invalid Paddle webhook body",
                    exception
            );
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid orderId in Paddle custom_data",
                    exception
            );
        }
    }
}