package com.yju.team2.seilomun.domain.review.entity;

import com.yju.team2.seilomun.domain.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ReviewPhoto> reviewPhotos = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "or_id")
    private Order order;

    @OneToOne(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private ReviewComment reviewComment;
}
