package com.yju.team2.seilomun.domain.report.entity;

import com.yju.team2.seilomun.domain.order.entity.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime created_at;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "or_id")
    private Order order;
}
