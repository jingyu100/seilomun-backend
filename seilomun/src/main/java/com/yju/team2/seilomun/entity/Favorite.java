package com.yju.team2.seilomun.entity;

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
@Table(name = "favorites")
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fa_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cu_id")
    private Customer customers;

    @ManyToOne
    @JoinColumn(name = "se_id")
    private Seller sellers;


}
