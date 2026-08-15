package com.moj.userservice.Repository;

import com.moj.userservice.Entity.BusinessContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessContextRepository extends JpaRepository<BusinessContext, Long> {
    Optional<BusinessContext> findByUsers_Id(UUID usersId);
}
