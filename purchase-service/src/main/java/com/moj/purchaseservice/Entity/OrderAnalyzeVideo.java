package com.moj.purchaseservice.Entity;

import com.moj.purchaseservice.enums.OrderStatus;
import com.moj.purchaseservice.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "order_analyze_video")
public class OrderAnalyzeVideo {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_analyze_video_id_generator")
    @SequenceGenerator(name = "order_analyze_video_id_generator", sequenceName = "order_analyze_video_id_seq", allocationSize = 1)
    @Column(nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "purchased_at")
    private Instant purchasedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

}
