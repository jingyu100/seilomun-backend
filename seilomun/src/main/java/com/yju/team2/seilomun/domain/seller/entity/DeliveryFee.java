package com.yju.team2.seilomun.domain.seller.entity;

import com.yju.team2.seilomun.domain.seller.dto.DeliveryFeeDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "delivery_fees")
public class DeliveryFee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "de_id")
    private Long id;

    @Column(name = "orders_money")
    private Integer ordersMoney;

    @Column(name = "delivery_tip")
    private Integer deliveryTip;

    @ManyToOne
    @JoinColumn(name="se_id")
    private Seller seller;


    // 업데이트용 메서드
    public void updateInformation(DeliveryFeeDto deliveryFeeDto) {
        this.ordersMoney = deliveryFeeDto.getOrdersMoney();
        this.deliveryTip = deliveryFeeDto.getDeliveryTip();
    }
}
