package com.moj.purchaseservice.Repository;

import com.moj.purchaseservice.Entity.OrderCredit;
import com.moj.purchaseservice.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderCreditRepository extends JpaRepository<OrderCredit, Long> {
    int deleteByIdAndUserIdAndStatus(Long id, UUID userId, OrderStatus status);
}
