package com.yju.team2.seilomun.domain.seller.repository;

import com.yju.team2.seilomun.domain.seller.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepository extends JpaRepository<Seller, Long> {
    //이메일이 db에 존재하는지 마는지
    boolean existsByEmail(String email);
}
