package com.yju.team2.seilomun.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "refunds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rf_id")
    private Long id;

    @Column(name = "refund_type", length = 255, nullable = false)
    private String refundType;

    @Column(name = "title", length = 30, nullable = false)
    private String title;

    @Column(name = "content", length = 1000, nullable = false)
    private String content;

    @Column(name = "status",nullable = false)
    private Character status;

    @Column(name = "requested_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime requestedAt;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @OneToOne
    @JoinColumn(name = "pa_id")
    private Payment payment;

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RefundPhoto> refundPhoto = new ArrayList<>();


}
