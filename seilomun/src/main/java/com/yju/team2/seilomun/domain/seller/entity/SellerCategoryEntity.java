package com.yju.team2.seilomun.domain.seller.entity;

import com.yju.team2.seilomun.domain.seller.enums.SellerCategory;
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
public class SellerCategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sc_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_name", nullable = false, length = 20)
    private SellerCategory categoryName;

}
