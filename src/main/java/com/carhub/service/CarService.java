package com.carhub.service;

import com.carhub.dto.CarDTO;
import com.carhub.dto.CarDetailResponse;
import com.carhub.dto.ReviewsDTO;
import com.carhub.entity.Car;
import com.carhub.repository.BrandRepository;
import com.carhub.repository.CarRepository;
import com.carhub.repository.ReviewsRepository;
import com.carhub.service.ai.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final BrandRepository brandRepository;
    private final ReviewsRepository reviewsRepository;
    private final VectorStoreService vectorStoreService;
    private final ReviewService reviewService;
    // Lấy tất cả xe
    public List<CarDTO> getAllCars() {
        return carRepository.findAll()
                .stream()
                .map(CarDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Lấy thông tin xe theo ID
    public CarDTO getCarById(Long id) {
        Optional<CarDTO> car = carRepository.findById(id)
                .map(CarDTO::fromEntity);
        return car.orElse(null);
    }
    // Lấy Reviews theo id xe
    // Lấy Các Mẫu Xe Tương Tự
    public List<CarDTO> getCarsSimilarByCarId(Long id) {
        return vectorStoreService.getCarsSimilar(id);
    }

    public CarDetailResponse getCarDetail(Long id){
        CarDetailResponse carDetail = new CarDetailResponse();
        carDetail.setCar(getCarById(id));
        carDetail.setReviews(reviewService.getReviewsByCarId(id));
        carDetail.setCarsSimilar(getCarsSimilarByCarId(id));
        return carDetail;
    }


    // Tìm xe theo hãng
    public List<CarDTO> getCarsByBrand(String brandName) {
        return carRepository.findByBrandName(brandName)
                .stream()
                .map(CarDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Tìm xe theo khoảng giá
    public List<CarDTO> getCarsByPriceRange(double minPrice, double maxPrice) {
        return carRepository.findByPriceBetween(minPrice, maxPrice)
                .stream()
                .map(CarDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Tìm xe theo giá sau khi giảm giá
    public List<CarDTO> getCarsByFinalPriceRange(double minPrice, double maxPrice) {
        return carRepository.findByFinalPriceBetween(minPrice, maxPrice)
                .stream()
                .map(CarDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Tìm xe theo năm sản xuất
    public List<CarDTO> getCarsByYear(int year) {
        return carRepository.findByManufactureYear(year)
                .stream()
                .map(CarDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Lấy xe còn hàng
    public List<CarDTO> getAvailableCars() {
        return carRepository.findByStockQuantityGreaterThan(0)
                .stream()
                .map(CarDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Tìm kiếm xe theo nhiều tiêu chí
    public List<CarDTO> searchCars(String brandName,Integer year, Double minPrice, Double maxPrice) {
        List<Car> cars;

        if (brandName != null && !brandName.isEmpty()) {
            cars = carRepository.findByBrandName(brandName);
        } else {
            cars = carRepository.findAll();
        }

        // Lọc theo giá nếu có
        if (minPrice != null && maxPrice != null) {
            cars = cars.stream()
                    .filter(car -> {
                        double finalPrice = car.getPrice() * (1 - car.getDiscount() / 100);
                        return finalPrice >= minPrice && finalPrice <= maxPrice;
                    })
                    .collect(Collectors.toList());
        }
        if (year != null) {
             cars = cars.stream()
                   .filter(car -> car.getManufactureYear() == year)
                   .collect(Collectors.toList());
        }

        return cars.stream()
                .map(CarDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Kiểm tra xe còn trong kho
    public boolean isCarAvailable(Long carId, int quantity) {
        Optional<Car> carOpt = carRepository.findById(carId);
        return carOpt.map(car -> car.getStockQuantity() >= quantity).orElse(false);
    }

    // Cập nhật số lượng xe trong kho
    @Transactional
    public boolean updateStock(Long carId, int quantity) {
        Optional<Car> carOpt = carRepository.findById(carId);
        if (carOpt.isPresent()) {
            Car car = carOpt.get();
            int newStock = car.getStockQuantity() - quantity;
            if (newStock >= 0) {
                car.setStockQuantity(newStock);
                carRepository.save(car);
                return true;
            }
        }
        return false;
    }

    public List<CarDTO> searchByModel(String model) {
        return carRepository.findByModelContainingIgnoreCase(model)
                .stream()
                .map(CarDTO::fromEntity)
                .collect(Collectors.toList());
    }
}