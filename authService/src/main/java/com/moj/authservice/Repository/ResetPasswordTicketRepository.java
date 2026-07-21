package com.moj.authservice.Repository;

import com.moj.authservice.Entity.ResetPasswordTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface ResetPasswordTicketRepository extends JpaRepository<ResetPasswordTicket, Long> {
    boolean existsByResetTokenAndExpirationDateAfter(byte[] resetToken, Instant expirationDate);
    Optional<ResetPasswordTicket> findByResetTokenAndExpirationDateAfter(byte[] resetToken, Instant expirationDate);
}
