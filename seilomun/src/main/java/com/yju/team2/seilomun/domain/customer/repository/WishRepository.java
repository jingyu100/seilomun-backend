package com.yju.team2.seilomun.domain.customer.repository;

import com.yju.team2.seilomun.domain.customer.entity.Customer;
import com.yju.team2.seilomun.domain.customer.entity.Wish;
import com.yju.team2.seilomun.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishRepository extends JpaRepository<Wish, Long> {
    List<Wish> findByCustomer(Customer customer);

    Page<Wish> findByCustomer(Customer customer, Pageable pageable);

    Optional<Wish> findByCustomerAndProduct(Customer customer, Product product);
}
