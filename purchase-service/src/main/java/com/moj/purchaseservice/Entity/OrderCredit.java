package com.moj.purchaseservice.Entity;

import com.moj.purchaseservice.enums.OrderStatus;
import com.moj.purchaseservice.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "order_credit")
public class OrderCredit {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    @Column(name = "credit", nullable = false)
    private Long credit;

    @Column(name = "price_in_usd", nullable = false)
    private BigDecimal priceInUsd;

    @Column(name = "order_id", nullable = false)
    private OrderStatus status;

    @Column(name = "purchased_at")
    private Instant purchasedAt;

    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
