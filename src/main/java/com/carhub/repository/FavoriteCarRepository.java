package com.carhub.repository;

import com.carhub.entity.Car;
import com.carhub.entity.FavoriteCar;
import com.carhub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteCarRepository extends JpaRepository<FavoriteCar, Long> {

    List<FavoriteCar> findByUserIdOrderByAddedAtDesc(Long userId);

    Optional<FavoriteCar> findByUserAndCar(User user, Car car);

    Optional<FavoriteCar> findByUserIdAndCarId(Long userId, Long carId);

    boolean existsByUserIdAndCarId(Long userId, Long carId);

    void deleteByUserIdAndCarId(Long userId, Long carId);

    @Query("SELECT fc.car FROM FavoriteCar fc WHERE fc.user.id = :userId ORDER BY fc.addedAt DESC")
    List<Car> findFavoriteCarsByUserId(@Param("userId") Long userId);
}