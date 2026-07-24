package com.moj.authservice.Repository;

import com.moj.authservice.Entity.ResetPasswordTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResetPasswordTicketRepository extends JpaRepository<ResetPasswordTicket, Long> {
    boolean existsByResetTokenAndExpirationDateAfter(byte[] resetToken, Instant expirationDate);
    Optional<ResetPasswordTicket> findByResetToken(byte[] resetToken);
    Optional<ResetPasswordTicket> findByUsersId(UUID id);
}
