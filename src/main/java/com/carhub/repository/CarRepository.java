package com.carhub.repository;

import com.carhub.entity.Brand;
import com.carhub.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByBrand(Brand brand);
    List<Car> findByBrandId(Long brandId);
    List<Car> findByModelContainingIgnoreCase(String model);
    List<Car> findByColor(String color);
    List<Car> findByManufactureYear(int year);
    List<Car> findByPriceBetween(double minPrice, double maxPrice);
    List<Car> findByStockQuantityGreaterThan(int quantity);

    //Tìm theo giá sau khi đã discount
    @Query("SELECT c FROM Car c WHERE c.price * (1 - c.discount) BETWEEN :minPrice AND :maxPrice")
    List<Car> findByFinalPriceBetween(@Param("minPrice") double minPrice, @Param("maxPrice") double maxPrice);

    @Query("SELECT c FROM Car c WHERE c.brand.name = :brandName")
    List<Car> findByBrandName(@Param("brandName") String brandName);
}