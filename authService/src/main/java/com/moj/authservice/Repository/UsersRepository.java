package com.moj.authservice.Repository;

import com.moj.authservice.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UsersRepository extends JpaRepository<Users, Long> {
    boolean existsByEmail(String email);

    Optional<Users> findById(UUID userId);

    @Query("SELECT u FROM Users u WHERE u.email = :email")
    Optional<Users> findUserByEmail(@Param("email") String email);
}
