package com.yju.team2.seilomun.domain.product.entity;

import com.yju.team2.seilomun.domain.customer.entity.Wish;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.order.entity.OrderItem;
import com.yju.team2.seilomun.domain.product.dto.ProductDto;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pr_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "original_price", nullable = false)
    private Integer originalPrice;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "status", length = 1)
    private Character status;

    @Column(name = "max_discount_rate", nullable = false)
    private Integer maxDiscountRate;

    @Column(name = "min_discount_rate", nullable = false)
    private Integer minDiscountRate;


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


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pc_id")
    private ProductCategory productCategory;


    public void updateProudct(ProductDto productDto,ProductCategory productCategory) {
        this.name = productDto.getName();
        this.description = productDto.getDescription();
        this.originalPrice = productDto.getOriginalPrice();
        this.stockQuantity = productDto.getStockQuantity();
        this.expiryDate = productDto.getExpiryDate();
        this.status = productDto.getStatus();
        this.minDiscountRate = productDto.getMinDiscountRate();
        this.maxDiscountRate = productDto.getMaxDiscountRate();
        this.createdAt = productDto.getCreatedAt();
        this.productCategory = productCategory;
    }

    public void updateStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
        if (this.stockQuantity == 0) {
            //End의 E
            this.status = 'E';
        }
    }

    public void updateStatus(Character status) {
        this.status = status;
    }
}
