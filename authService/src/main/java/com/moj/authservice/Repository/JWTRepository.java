package com.moj.authservice.Repository;

import com.moj.authservice.Entity.Jwt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JWTRepository extends JpaRepository<Jwt, Long> {
    Optional<Jwt> findByUsersId(UUID id);
    Optional<Jwt> findByRefreshToken(String RefreshToken);
    void deleteAllByUsersId(UUID id);
}
