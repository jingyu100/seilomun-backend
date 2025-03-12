package com.yju.team2.seilomun.domain.seller.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "seller_photos")
public class SellerPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sp_id")
    private Long id;

    @Column(name = "photo_url", nullable = false, length = 100,unique = true)
    private String photoUrl;

    @ManyToOne
    @JoinColumn(name="se_id")
    private Seller seller;
}
