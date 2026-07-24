package com.moj.authservice.Repository;

import com.moj.authservice.Entity.TwoFactor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TwoFactorRepository extends JpaRepository<TwoFactor, Long> {
    Optional<TwoFactor> findByUsersId(UUID id);
}
