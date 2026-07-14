package com.moj.authservice.Entity;

import com.moj.authservice.Enums.TwoFactorType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "two_factor")
public class TwoFactor {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private Integer twoFactorCode;

    private Instant ExpirationDate;

    @Enumerated(EnumType.STRING)
    private TwoFactorType twoFactorType;

    @OneToOne
    @JoinColumn(name = "users_id")
    private Users users;
}
