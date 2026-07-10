package com.moj.authservice.Entity;

import com.moj.authservice.Enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String fullName;

    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private LocalDate createdDate;

    private Long creditSum;

    private boolean userVerifiedEmailw;

    @OneToOne
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;


}
