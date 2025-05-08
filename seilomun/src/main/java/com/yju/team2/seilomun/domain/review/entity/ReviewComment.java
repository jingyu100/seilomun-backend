package com.yju.team2.seilomun.domain.review.entity;

import com.yju.team2.seilomun.domain.seller.entity.Seller;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "review_comments")
public class ReviewComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rc_id")
    private Long id;

    @Column(name = "content",nullable = false,length = 1000)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at",nullable = false)
    private LocalDateTime created_at;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "re_id")
    private Review review;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "se_id")
    private Seller seller;
}
