package com.moj.authservice.Repository;

import com.moj.authservice.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsersRepository extends JpaRepository<Users, Long> {
    boolean existsByEmail(String email);
    boolean existsByEmailAndPassword(String email, String password);

    Optional<Object> findById(UUID userId);
}
