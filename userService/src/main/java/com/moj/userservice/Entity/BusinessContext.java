package com.moj.userservice.Entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "business_context")
public class BusinessContext {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "business_context_generator")
    @SequenceGenerator(name = "business_context_generator", sequenceName = "business_context_sequence", allocationSize = 1)
    private Long id;

    @Column(nullable = false, name = "business_name")
    private String businessName;

    @Column(nullable = false, name = "niche")
    private String niche;

    private String description;

    @Column(name = "target_audience")
    private String targetAudience;


    @OneToOne
    @JoinColumn(name = "users_id", unique = true, nullable = false)
    private Users users;
}
