package com.moj.authservice.Repository;

import com.moj.authservice.Entity.TwoFactor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TwoFactorRepository extends JpaRepository<TwoFactor, Long> {
    Optional<TwoFactor> findByUsersId(UUID id);
}
