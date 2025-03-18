package com.yju.team2.seilomun.domain.seller.entity;

import com.yju.team2.seilomun.domain.chat.entity.ChatRoom;
import com.yju.team2.seilomun.domain.event.entity.Event;
import com.yju.team2.seilomun.domain.order.entity.Order;
import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.review.entity.ReviewComment;
import com.yju.team2.seilomun.dto.SellerInformationDto;
import com.yju.team2.seilomun.validation.ValidPassword;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "sellers")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "se_id")
    private Long id;

    @Column(name = "business_number", nullable = false, length = 10)
    private String businessNumber;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 100)
    @ValidPassword
    private String password;

    @Column(name = "store_name", nullable = false, length = 20)
    private String storeName;

    @Column(name = "category", nullable = false, length = 10)
    private String category;

    @Column(name = "store_description")
    private String storeDescription;

    @Column(name = "notification")
    private String notification;

    @Column(name = "phone", nullable = false, length = 11)
    private String phone;

    @Column(name = "post_code", nullable = false, length = 5)
    private String postCode;

    @Column(name = "address_detail", length = 20)
    private String addressDetail;

    @Column(name = "operating_hours", nullable = false)
    private String operatingHours;

    @Column(name = "delivery_available", nullable = false, length = 1)
    private Character deliveryAvailable;

    @Column(name = "min_order_amount", length = 6)
    private String minOrderAmount;

    @Column(name = "delivery_area")
    private String deliveryArea;

    @Column(name = "rating", nullable = false)
    private Float rating;

    @Column(name = "pickup_time", nullable = false)
    private String pickupTime;

    @Column(name = "is_open", nullable = false, length = 1)
    private Character isOpen;

    @Column(name = "status", nullable = false, length = 1)
    private Character status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SellerPhoto> sellerPhotos = new ArrayList<>();

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Event> events = new ArrayList<>();

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DeliveryFee> deliveryFees = new ArrayList<>();

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChatRoom> chatRooms = new ArrayList<>();


    @OneToOne(mappedBy = "seller", cascade = CascadeType.ALL /*fetch = FetchType.LAZY*/)
    private ReviewComment reviewComment;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sc_id")
    private SellerCategory sellerCategory;


    // 업데이트용 메서드
    public void updateInformation(SellerInformationDto sellerInformationDto) {
        this.storeName = sellerInformationDto.getStoreName();
        this.storeDescription = sellerInformationDto.getStoreDescription();
        this.notification = sellerInformationDto.getNotification();
        this.deliveryAvailable = sellerInformationDto.getDeliveryAvailable();
        this.minOrderAmount = sellerInformationDto.getMinOrderAmount();
        this.deliveryArea = sellerInformationDto.getDeliveryArea();
        this.operatingHours = sellerInformationDto.getOperatingHours();
        this.category = sellerInformationDto.getCategory();
        this.phone = sellerInformationDto.getPhone();
        this.pickupTime = sellerInformationDto.getPickupTime();
    }
}