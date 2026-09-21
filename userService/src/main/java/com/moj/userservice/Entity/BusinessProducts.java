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
@Table(name = "business_products")
public class BusinessProducts {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "business_products_generator")
    @SequenceGenerator(name = "business_products_generator", sequenceName = "business_products_sequence", allocationSize = 1)
    private Long id;

    private String productName;

    private String productDescription;

    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "users_id", nullable = false)
    private Users users;

}
