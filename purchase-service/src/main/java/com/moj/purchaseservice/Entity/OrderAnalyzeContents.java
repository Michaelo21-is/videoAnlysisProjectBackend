package com.moj.purchaseservice.Entity;

import com.moj.purchaseservice.enums.ContentType;
import com.moj.purchaseservice.enums.OrderAnalyzeVideoStatus;
import com.moj.purchaseservice.enums.SumOfContent;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "order_analyze_content")
public class OrderAnalyzeContents {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sum_of_content", nullable = false)
    private SumOfContent sumOfContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private ContentType contentType;

    @Column(name = "credit_cost", nullable = false)
    private Long creditCost;

    @Column(name = "purchased_at")
    private Instant purchasedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderAnalyzeVideoStatus status;
}
