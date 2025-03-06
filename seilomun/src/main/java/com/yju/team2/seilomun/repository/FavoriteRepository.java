package com.yju.team2.seilomun.repository;

import com.yju.team2.seilomun.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {
}
