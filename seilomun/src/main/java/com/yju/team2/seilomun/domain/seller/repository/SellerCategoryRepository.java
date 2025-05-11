package com.yju.team2.seilomun.domain.seller.repository;

import com.yju.team2.seilomun.domain.seller.entity.SellerCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerCategoryRepository extends JpaRepository<SellerCategoryEntity, Long> {
    boolean existsByCategoryName(String categoryName);
}
