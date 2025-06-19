package com.yju.team2.seilomun.domain.seller.repository;

import com.yju.team2.seilomun.domain.seller.entity.Seller;
import com.yju.team2.seilomun.domain.seller.entity.SellerPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerPhotoRepository extends JpaRepository<SellerPhoto, Long> {
    long countBySeller(Seller seller);

    Optional<SellerPhoto> findTopBySellerOrderById(Seller seller);
}
