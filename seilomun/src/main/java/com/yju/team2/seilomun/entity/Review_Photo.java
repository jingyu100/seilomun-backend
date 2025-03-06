package com.yju.team2.seilomun.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "review_photos")
@NoArgsConstructor
@AllArgsConstructor
public class Review_Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rp_id")
    private Long id;

    @Column(name = "photo_url", nullable = false , length = 255, unique = true)
    private String photo_url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "re_id")
    private Review review;
}
