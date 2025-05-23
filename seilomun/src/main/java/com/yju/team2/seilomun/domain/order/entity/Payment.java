package com.yju.team2.seilomun.domain.order.entity;

import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.order.dto.PaymentResDto;
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

    @Column(name = "paid_at" , nullable = false)
    @CreationTimestamp
    private LocalDateTime paidAt;

    @Column(name = "refund_status", nullable = false)
    private String refundStatus;

    @ManyToOne
    @JoinColumn(name = "or_id")
    private Order order;

    // 결제 성공 여부
    @Column
    private boolean paySuccessYN;
    // 실패하면 사유
    @Column
    private String failReason;
    // 결제 취소 여부
    @Column
    private boolean cancelYN;
    // 결제키
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
    public void successPayment(String paymentKey){
        this.paymentKey = paymentKey;
        this.paySuccessYN = true;
     }
     public void failPayment(boolean paySuccessYN){
        this.paySuccessYN = paySuccessYN;
     }
     public void insertFailReason(String failReason){
        this.failReason = failReason;
     }
     public void cancelYN(boolean cancelYN){
        this.cancelYN = cancelYN;
     }
}
