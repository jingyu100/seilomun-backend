package com.yju.team2.seilomun.domain.order.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "refund_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rfp_id")
    private Long id;

    @Column(name = "photo_url",length = 100,nullable = false)
    private String photoUrl;

    @ManyToOne
    @JoinColumn(name = "rf_id")
    private Refund refund;
}
