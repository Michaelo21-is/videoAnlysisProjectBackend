package com.moj.authservice.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
@Table(name = "reset_password_ticket")
public class ResetPasswordTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reset_password_ticket_id_generator")
    @SequenceGenerator(name = "reset_password_ticket_id_generator", sequenceName = "reset_password_ticket_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, name = "reset_token")
    private byte[] resetToken;

    @Column(nullable = false, name = "expiration_date")
    private Instant expirationDate;

    @OneToOne
    @JoinColumn(name = "users_id")
    private Users users;

}
