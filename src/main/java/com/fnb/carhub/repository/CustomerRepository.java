package com.fnb.carhub.repository;

import com.fnb.carhub.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUsername(String username);
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByPhoneNumber(String phoneNumber);
    List<Customer> findByShippingAddressContainingIgnoreCase(String address);
    List<Customer> findByFullNameContainingIgnoreCase(String fullName);
}