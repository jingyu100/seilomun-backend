package com.yju.team2.seilomun.domain.product.entity;

import com.yju.team2.seilomun.domain.customer.entity.Wish;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.order.entity.OrderItem;
import com.yju.team2.seilomun.dto.ProductDto;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Duration;
import java.time.LocalDate;
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

    //상품사진 주소
    @Column(name = "thumbnail_url", length = 100)
    private String thumbnailUrl;

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


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pc_id")
    private ProductCategory productCategory;

    //할인율 계산메서드
    public Integer calculateDiscountRate() {
        if (expiryDate == null || minDiscountRate == null || maxDiscountRate == null)
            return 0;

        //현재 시간이 만료일을 지났는지 확인
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(expiryDate)) {
            return minDiscountRate;
        }

        long totalDays = Duration.between(now, expiryDate).toDays();
        log.info("유통기한까지 남은 일수 계산 : " + totalDays);

        if(totalDays < 0)
            totalDays = 0;
        if (totalDays <= 3)
            return maxDiscountRate;
        else {
            double discountRate = minDiscountRate + (double) (maxDiscountRate - minDiscountRate) * (1.0 -(double) totalDays / (double) totalDaysInMonth(expiryDate));

            discountRate = Math.max(minDiscountRate, Math.min(maxDiscountRate, discountRate));

            return (int) Math.round(discountRate);
        }

    }

    // 유통기한이 있는 달의 마지막 날까지의 총 일수 계산
    private long totalDaysInMonth(LocalDateTime expiryDate) {
        log.info("유통기한의 마지막 날 : " + String.valueOf(expiryDate.toLocalDate().lengthOfMonth()));
        return expiryDate.toLocalDate().lengthOfMonth();
    }


    public void updateProudct(ProductDto productDto) {
        this.name = productDto.getName();
        this.description = productDto.getDescription();
        this.thumbnailUrl = productDto.getThumbnailUrl();
        this.originalPrice = productDto.getOriginalPrice();
        this.stockQuantity = productDto.getStockQuantity();
        this.expiryDate = productDto.getExpiryDate();
        this.status = productDto.getStatus();
        this.minDiscountRate = productDto.getMinDiscountRate();
        this.maxDiscountRate = productDto.getMaxDiscountRate();
        this.createdAt = productDto.getCreatedAt();
        this.productCategory = productDto.getProductCategory();
    }
}
