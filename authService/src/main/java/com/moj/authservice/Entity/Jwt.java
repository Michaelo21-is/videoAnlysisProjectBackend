package com.moj.authservice.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Builder
@Table(name = "jwt")

public class Jwt {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "jwt_id_generator")
    @SequenceGenerator(name = "jwt_id_generator", sequenceName = "jwt_id_seq", allocationSize = 1)
    private Long id;
    @Column(nullable = false, name = "refresh_token" )
    private String refreshToken;

    @Column(nullable = false, name = "expiration_date")
    private Instant expirationDate;

    @OneToOne
    @JoinColumn(name = "users_id")
    private Users users;
}
