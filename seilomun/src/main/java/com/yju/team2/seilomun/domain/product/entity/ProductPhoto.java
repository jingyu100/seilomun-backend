package com.yju.team2.seilomun.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pp_id")
    private Long id;

    @Column(name = "photo_url",length = 100,nullable = false)
    private String photoUrl;

    @ManyToOne
    @JoinColumn(name = "pr_id")
    private Product product;
}
