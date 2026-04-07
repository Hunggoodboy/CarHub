package com.carhub.repository;

import com.carhub.entity.Brand;
import com.carhub.entity.Car;
import com.carhub.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByBrand(Brand brand);
    List<Car> findByBrandId(Long brandId);
    List<Car> findByModelContainingIgnoreCase(String model);
    List<Car> findByColor(String color);
    List<Car> findByManufactureYear(int year);
    List<Car> findByPriceBetween(double minPrice, double maxPrice);
    List<Car> findByStockQuantityGreaterThan(int quantity);
    Optional<Car> findById(Long id);

    @Query("SELECT c.seller FROM Car c WHERE c.id = :carId")
    User findSellerById(@Param("carId") Long carId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Car c WHERE c.id = :id")
    Optional<Car> findByIdForUpdate(@Param("id") Long id);
    //Tìm theo giá sau khi đã discount
    @Query("SELECT c FROM Car c WHERE c.price * (1 - c.discount) BETWEEN :minPrice AND :maxPrice")
    List<Car> findByFinalPriceBetween(@Param("minPrice") double minPrice, @Param("maxPrice") double maxPrice);

    @Query("SELECT c FROM Car c WHERE c.brand.name = :brandName")
    List<Car> findByBrandName(@Param("brandName") String brandName);

    @Query("""
            SELECT c
            FROM Car c
            LEFT JOIN FETCH c.brand b
            LEFT JOIN FETCH c.seller s
            WHERE (:brandId IS NULL OR b.id = :brandId)
              AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(COALESCE(c.model, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY c.id DESC
            """)
    List<Car> searchForAdmin(@Param("keyword") String keyword, @Param("brandId") Long brandId);

    @Query("SELECT COUNT(c) > 0 FROM Car c WHERE c.seller.id = :sellerId")
    boolean existsBySellerId(@Param("sellerId") Long sellerId);

    @Query(value = "SELECT c.imageUrl FROM Car c WHERE c.id = :id")
    String findImageUrlById(@Param("id") Long id);

}
