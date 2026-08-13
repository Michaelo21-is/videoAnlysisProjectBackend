package com.moj.purchaseservice.Repository;

import com.moj.purchaseservice.Entity.OrderAnalyzeVideo;
import com.moj.purchaseservice.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface OrderAnalyzeVideoRepository
        extends JpaRepository<OrderAnalyzeVideo, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE OrderAnalyzeVideo o
            SET o.status = :newStatus,
                o.purchasedAt = :purchasedAt
            WHERE o.id = :id
              AND o.status = :expectedStatus
            """)
    int updateStatusIfCurrent(
            @Param("id") Long id,
            @Param("expectedStatus") OrderStatus expectedStatus,
            @Param("newStatus") OrderStatus newStatus,
            @Param("purchasedAt") Instant purchasedAt
    );
}