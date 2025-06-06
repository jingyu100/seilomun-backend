package com.yju.team2.seilomun.domain.customer.repository;

import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.entity.Favorite;
import com.yju.team2.seilomun.domain.seller.entity.Seller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByCustomer(Customer customer);
    List<Favorite> findBySellerId(Long sellerId);

    Page<Favorite> findByCustomer(Customer customer, Pageable pageable);

    Optional<Favorite> findByCustomerAndSeller(Customer customer, Seller seller);
}
