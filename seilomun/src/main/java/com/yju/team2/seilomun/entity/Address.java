package com.yju.team2.seilomun.entity;

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
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ad_id")
    private Long id;

    @Column(name = "post_code", length = 5, nullable = false)
    private String postCode;

    @Column(name = "address_detail", length = 20)
    private String addressDetail;

    @Column(name = "address_main",nullable = false)
    private Character addressMain;

    @Column(name = "label", length = 10, nullable = false)
    private String label;

    @ManyToOne
    @JoinColumn(name = "cu_id")
    private Customer customer;
}
