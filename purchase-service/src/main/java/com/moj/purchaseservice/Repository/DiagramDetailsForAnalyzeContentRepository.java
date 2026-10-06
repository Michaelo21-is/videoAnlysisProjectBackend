package com.moj.purchaseservice.Repository;

import com.moj.purchaseservice.Entity.DiagramDetailsForAnalyzeContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DiagramDetailsForAnalyzeContentRepository extends JpaRepository<DiagramDetailsForAnalyzeContent, Long> {
    Page<DiagramDetailsForAnalyzeContent> findAllByUserId(UUID userId, Pageable pageable);
}
