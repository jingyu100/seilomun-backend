package com.yju.team2.seilomun.domain.product.entity;

import com.yju.team2.seilomun.domain.customer.entity.Wish;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.order.entity.OrderItem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pr_id")
    private Long id;

    @Column(name = "name" , nullable = false, length = 20)
    private String name;

    @Column(name = "category", nullable = false, length = 10)
    private String category;

    @Column(name = "description")
    private String description;
    
    //상품사진 주소
    @Column(name = "thumbnail_url",length = 100)
    private String thumbnailUrl;
    
    @Column(name = "original_price", nullable = false)
    private Integer originalPrice;

    @Column(name = "discount_price")
    private Integer discountPrice;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "status",length = 1)
    private Character status;

    @Column(name = "max_discount_rate", nullable = false    )
    private Integer maxDiscountRate;

    @Column(name = "min_discount_rate", nullable = false)
    private Integer minDiscountRate;

    @Column(name = "current_discount_rate")
    private Integer currentDiscountRate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "se_id")
    private Seller seller;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductPhoto> productPhotos = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Wish> wishes = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();
}
