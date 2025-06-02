package com.yju.team2.seilomun.domain.customer.repository;

import com.yju.team2.seilomun.domain.customer.entity.Address;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    
    // 모든 주소 조회
    List<Address> findByCustomerId(Long customerId);
    
    // 대표 주소 조회
    Optional<Address> findByCustomerIdAndAddressMain(Long customerId, Character addressMain);
    
    // 대표 주소 예외 처리
    long countByCustomerIdAndAddressMain(Long customerId, Character addressMain);
}
