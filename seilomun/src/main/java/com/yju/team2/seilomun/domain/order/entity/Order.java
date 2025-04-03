package com.yju.team2.seilomun.domain.order.entity;


import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@Table(name = "orders")
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "or_id")
    private Long orId;

    @Column(name = "order_name", nullable = false, length = 100,unique = true)
    private String orderName;

    @Column(name = "total_amount",nullable = false)
    private Integer totalAmount;

    @Column(name = "used_points",nullable = false)
    private Integer usedPoints;

    @Column(name = "memo",nullable = true,length = 50)
    private String memo;

    @Column(name = "is_delivery",nullable = false)
    private Character isDelivery;

    @Column(name = "delivery_address",nullable = true)
    private String deliveryAddress;

    @Column(name = "delivery_status",nullable = true)
    private Character deliveryStatus;

    @Column(name = "is_reviewed",nullable = false)
    private Character isReivewed;

    @Column(name = "order_status",nullable = false)
    private Character orderStatus;

    @CreationTimestamp
    @Column(name = "created_at",nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cu_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "se_id")
    private Seller seller;
}
