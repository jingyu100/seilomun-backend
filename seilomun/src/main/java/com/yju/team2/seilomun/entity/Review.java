package com.yju.team2.seilomun.entity;

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
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "re_id")
    private Long id;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "content" , nullable = false, length = 1000)
    private String content;

    @Column(name = "created_at",nullable = false)
    private LocalDateTime createdAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "or_id")
    private Order order;
}
