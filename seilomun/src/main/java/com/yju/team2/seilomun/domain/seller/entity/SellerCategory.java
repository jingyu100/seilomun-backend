package com.yju.team2.seilomun.domain.seller.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seller_category")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sc_id")
    private Long id;

    @Column(name = "category_name", nullable = false, length = 10)
    private String categoryName;

}
