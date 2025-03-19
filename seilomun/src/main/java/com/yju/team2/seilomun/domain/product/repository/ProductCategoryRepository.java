package com.yju.team2.seilomun.domain.product.repository;

import com.yju.team2.seilomun.domain.product.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory,Long> {
}
