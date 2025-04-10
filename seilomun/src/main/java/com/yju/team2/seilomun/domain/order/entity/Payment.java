package com.yju.team2.seilomun.domain.order.entity;

import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.dto.OrderDto;
import com.yju.team2.seilomun.dto.PaymentResDto;
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

    @Column(nullable = false, name = "pay_name")
    private String payName;

    private boolean paySuccessYN;

    @Column(name = "paid_at" , nullable = false)
    @CreationTimestamp
    private LocalDateTime paidAt;

    @Column(name = "refund_status", nullable = false)
    private String refundStatus;

    @ManyToOne
    @JoinColumn(name = "or_id")
    private Order order;


    // 일단 넣어놓음
    @Column
    private String failReason;
    @Column
    private boolean cancelYN;

    @Column
    private String paymentKey;
    public PaymentResDto toPaymentResDto(Customer customer ) { // DB에 저장하게 될 결제 관련 정보들
        return PaymentResDto.builder()
                .payType(paymentMethod)
                .amount(totalAmount)
                .orderName(payName)
                .orderId(transactionId)
                .customerEmail(customer.getEmail())
                .customerName(customer.getName())
                .createdAt(String.valueOf(paidAt))
                .cancelYN(cancelYN)
                .failReason(failReason)
                .build();
    }

}
