package com.yju.team2.seilomun.domain.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pa_id")
    private Long id;

    @Column(name = "transaction_id",length = 36, nullable = false)
    private String transactionId;

    @Column(name = "payment_method", length = 10, nullable = false)
    private String paymentMethod;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    @Column(name = "paid_at" , nullable = false)
    @CreationTimestamp
    private LocalDateTime paidAt;

    @Column(name = "refund_status", nullable = false)
    private String refundStatus;

    @ManyToOne
    @JoinColumn(name = "or_id")
    private Order order;

}
