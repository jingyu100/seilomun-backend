package com.yju.team2.seilomun.repository;

import com.yju.team2.seilomun.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
