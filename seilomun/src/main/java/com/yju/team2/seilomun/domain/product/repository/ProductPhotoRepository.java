package com.yju.team2.seilomun.domain.product.repository;

import com.yju.team2.seilomun.domain.product.entity.Product;
import com.yju.team2.seilomun.domain.product.entity.ProductPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductPhotoRepository extends JpaRepository<ProductPhoto, Long> {
    void deleteByProduct(Product product);

    Optional<ProductPhoto> findTopByProductOrderById(Product product);

    List<ProductPhoto> findByProduct(Product product);
}
