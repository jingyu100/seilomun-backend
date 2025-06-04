package com.yju.team2.seilomun.domain.report.entity;

import com.yju.team2.seilomun.domain.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rp_id")
    private Long id;

    @Column(name = "type",nullable = false, length = 10)
    private String type;

    @Column(name = "content", nullable = false, length = 100)
    private String content;

    @Column(name = "target_type", nullable = false)
    private Character target_type;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime created_at;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "or_id")
    private Order order;
}
