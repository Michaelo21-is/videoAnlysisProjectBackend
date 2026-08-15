package com.moj.userservice.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users")
public class Users {
    @Id
    private UUID id;

    private String email;

    private String fullName;

    private Long creditSum;

    @Column(name = "time_zone")
    private String timeZone;
}
