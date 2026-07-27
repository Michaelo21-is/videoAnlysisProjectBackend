package com.moj.purchaseservice.Service;

import com.moj.purchaseservice.Entity.OrderAnalyzeContents;
import com.moj.purchaseservice.Repository.OrderAnalyzeContentsRepository;
import com.moj.purchaseservice.Response.OrderAnalyzeStatusResponse;
import com.moj.purchaseservice.Utils.CheckOrderStatus;
import com.moj.purchaseservice.enums.OrderAnalyzeVideoStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>(); // holding all the order id connetion here
    private final OrderAnalyzeContentsRepository orderAnalyzeContentsRepository;

    public SseService(OrderAnalyzeContentsRepository orderAnalyzeContentsRepository) {
        this.orderAnalyzeContentsRepository = orderAnalyzeContentsRepository;
    }

    public SseEmitter orderStatus(Long orderId) {
        SseEmitter emitter =
                new SseEmitter(10 * 60 * 1000L); ;// 10 min connection if time pass closing it

        SseEmitter previousEmitter =
                emitters.put(orderId, emitter);

        if (previousEmitter != null) {
            previousEmitter.complete(); // if older order id is open in other browser i am closing and open new one
        }

        emitter.onCompletion(() ->
                emitters.remove(orderId, emitter)
        ); // after completing the task removing it from the hashMap

        emitter.onTimeout(() -> {
            emitters.remove(orderId, emitter);
            emitter.complete();
        }); // after time pass also removing it from hashmap

        emitter.onError(error ->
                emitters.remove(orderId, emitter)
        );

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("connected")
                            .data(Map.of(
                                    "orderId", orderId,
                                    "message", "Listening for order status"
                            ))
            ); // first connection handshake to the front letting it know the connection succeed
        } catch (IOException exception) {
            emitters.remove(orderId, emitter);
            emitter.completeWithError(exception);
        }

        return emitter;
    }
    // when the listenr get the message from the user-service it update the user aswell
    public void sendFinalStatus(Long orderId, OrderAnalyzeStatusResponse response) {

        SseEmitter emitter = emitters.remove(orderId);

        if (emitter == null) {
            return;
        }

        try {
            emitter.send(
                    SseEmitter.event()
                            .id(orderId.toString())
                            .name("analyze-content-status")
                            .data(response)
            );

            emitter.complete();

        } catch (IOException | IllegalStateException exception) {
            emitter.completeWithError(exception);
        }
    }

    public boolean isConnected(Long orderId) {
        return emitters.containsKey(orderId);
    }


    /// ***
    /// order analyze content sse
    /// ***
    public SseEmitter OrderAnalyzeStatusSSE(Long orderId, UUID userId) {
        OrderAnalyzeContents order =
                orderAnalyzeContentsRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Order not found"
                                )
                        );

        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot access this order"
            );
        }

        SseEmitter emitter = orderStatus(orderId);

        if (order.getStatus() == OrderAnalyzeVideoStatus.PURCHASED) {
            // checking if already got the message from user service
            OrderAnalyzeStatusResponse response = CheckOrderStatus.createStatusResponse(order);
            // sending it to the front
            sendFinalStatus(orderId, response);
        }

        return emitter;
    }
    /// ***
    /// order analyze content sse
    /// ***
}
