package com.moj.userservice.Entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "business_avatar")
public class BusinessAvatar {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "business_avatar_generator")
    @SequenceGenerator(name = "business_avatar_generator", sequenceName = "business_avatar_sequence", allocationSize = 1)
    private Long id;

    private String s3ImageKey;

    private String avatarName;

    private String avatarDescription;

    @ManyToOne
    @JoinColumn(name = "users_id", unique = true, nullable = false)
    private Users users;
}
