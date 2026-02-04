package com.fnb.carhub.repository;

import com.fnb.carhub.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    Optional<Brand> findByName(String name);
    List<Brand> findByOrigin(String origin);
    List<Brand> findByNameContainingIgnoreCase(String name);
}