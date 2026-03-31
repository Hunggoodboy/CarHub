package com.carhub.repository;

import com.carhub.entity.CarImagesSub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarImagesSubRepository extends JpaRepository<CarImagesSub, Long> {
    @Query("SELECT c.imageUrl from CarImagesSub c where c.car.id = :carId order by c.sortOrder asc")
    List<String> findAllImageUrlsByCarId(Long carId);
}
