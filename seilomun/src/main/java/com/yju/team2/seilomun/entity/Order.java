package com.yju.team2.seilomun.entity;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.Id;

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
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "or_id")
    private Long or_id;

    @Column(name = "order_name", nullable = false, length = 100,unique = true)
    private String order_number;

    @Column(name = "total_amount",nullable = false)
    private Integer total_amount;

    @Column(name = "used_points",nullable = false)
    private Integer used_points;

    @Column(name = "memo",nullable = true,length = 50)
    private String memo;

    @Column(name = "is_delivery",nullable = false)
    private Character is_delivery;

    @Column(name = "delivery_address",nullable = true)
    private String delivery_address;

    @Column(name = "delivery_status",nullable = false)
    private Character delivery_status;

    @Column(name = "is_reviewed",nullable = false)
    private Character is_reivewed;

    @Column(name = "order_status",nullable = false)
    private Character order_status;

    @Column(name = "created_at",nullable = false)
    private LocalDateTime created_at;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cu_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "se_id")
    private Seller sellers;
}
