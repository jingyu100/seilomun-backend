package com.yju.team2.seilomun.domain.notification.entity;

import com.yju.team2.seilomun.domain.seller.entity.Seller;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "notification_photos")
public class NotificationPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "np_id")
    private Long id;

    @Column(name = "photo_url", nullable = false, length = 100)
    private String photoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="se_id")
    private Seller seller;
}
