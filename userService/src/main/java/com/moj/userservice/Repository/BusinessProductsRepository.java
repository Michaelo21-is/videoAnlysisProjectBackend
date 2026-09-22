package com.moj.userservice.Repository;

import com.moj.userservice.Entity.BusinessProducts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface BusinessProductsRepository extends JpaRepository<BusinessProducts, Long> {
    Page<BusinessProducts> findAllByUsers_Id(UUID userId, Pageable pageable);
}
