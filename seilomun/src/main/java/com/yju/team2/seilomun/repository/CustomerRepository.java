package com.yju.team2.seilomun.repository;

import com.yju.team2.seilomun.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
}
