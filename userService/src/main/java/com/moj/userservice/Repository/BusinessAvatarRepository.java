package com.moj.userservice.Repository;

import com.moj.userservice.Entity.BusinessAvatar;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.UUID;

@Repository
public interface BusinessAvatarRepository extends JpaRepository<BusinessAvatar, Long> {
    Page<BusinessAvatar> findAllByUsers_Id(UUID userId, Pageable pageable);
}
