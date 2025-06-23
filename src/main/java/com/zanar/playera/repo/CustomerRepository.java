package com.zanar.playera.repo;

import com.zanar.playera.entity.Customer;
import com.zanar.playera.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}