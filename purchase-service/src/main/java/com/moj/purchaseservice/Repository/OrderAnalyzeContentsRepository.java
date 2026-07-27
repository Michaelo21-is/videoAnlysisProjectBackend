package com.moj.purchaseservice.Repository;

import com.moj.purchaseservice.Entity.OrderAnalyzeContents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderAnalyzeContentsRepository extends JpaRepository<OrderAnalyzeContents, Long> {
}
