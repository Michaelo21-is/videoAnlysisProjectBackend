package com.moj.purchaseservice.Repository;

import com.moj.purchaseservice.Entity.OrderAnalyzeContents;
import com.moj.purchaseservice.enums.OrderAnalyzeContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface OrderAnalyzeContentsRepository extends JpaRepository<OrderAnalyzeContents, Long> {
}
