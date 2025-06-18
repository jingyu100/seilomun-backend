package com.yju.team2.seilomun.domain.customer.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(name = "address", length = 200, nullable = false)
    private String address;

    @Column(name = "address_detail", length = 20)
    private String addressDetail;

    @Column(name = "address_main",nullable = false)
    private Character addressMain;

    @Column(name = "label", length = 10, nullable = false)
    private String label;

    @ManyToOne
    @JoinColumn(name = "cu_id")
    @JsonIgnore
    private Customer customer;

    public void updateAddress(String address, String detail, Character addressMain, String label) {
        this.address = address;
        this.addressDetail = detail;
        this.addressMain = addressMain;
        this.label = label;
    }

    public void updateMain(Character addressMain) {
        this.addressMain = addressMain;
    }
}
