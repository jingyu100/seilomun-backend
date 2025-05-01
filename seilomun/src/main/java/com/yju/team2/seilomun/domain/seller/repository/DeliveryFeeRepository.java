package com.yju.team2.seilomun.domain.seller.repository;

import com.yju.team2.seilomun.domain.seller.entity.DeliveryFee;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryFeeRepository extends JpaRepository<DeliveryFee, Long> {

    List<DeliveryFee> findBySeller(Seller seller);
}
