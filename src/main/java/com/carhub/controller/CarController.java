package com.carhub.controller;

import com.carhub.entity.User;
import com.carhub.dto.CarDTO;
import com.carhub.dto.Response.CarDetailResponse;
import com.carhub.dto.ReviewsDTO;
import com.carhub.service.Car.CarService;
import com.carhub.service.Car.OrderService;
import com.carhub.service.Car.ReviewService;

import com.carhub.service.authentication.UserService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;
    private final ReviewService reviewService;
    private final OrderService orderService;
    private final UserService userService;
    @GetMapping
    public ResponseEntity<List<CarDTO>> getAllCars() {

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        List<CarDTO> cars;

        if (authentication == null || authentication.getName().equals("anonymousUser")) {
            cars = carService.getAllCars();
        } else {
            String username = authentication.getName();

            User user = userService.findByUsername(username);
            Long userId = user.getId();

            cars = carService.getCarsExcludeSeller(userId);
        }

        return ResponseEntity.ok(cars);
    }

    /**
     * Lấy thông tin chi tiết xe theo ID
     * GET /api/cars/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CarDetailResponse> getCarById(@PathVariable Long id) {
        CarDetailResponse car = carService.getCarDetail(id);
        return ResponseEntity.ok(car);
    }


    @RestController
    @RequestMapping("/api/reviews")
    @AllArgsConstructor
    public class ReviewController {
        @PostMapping("/{id}")
         public ResponseEntity<Void> createdReviews(@PathVariable Long id,
                                               @RequestBody ReviewsDTO reviewsDTO) {

            reviewService.createReview(reviewsDTO, id);

            return ResponseEntity.noContent().build();
        }
    }

    /**
     * Tìm kiếm xe theo model
     * GET /api/cars/search?model=xyz
     */
    @GetMapping("/search")
    public ResponseEntity<List<CarDTO>> searchByModel(@RequestParam String model) {
        List<CarDTO> cars = carService.searchByModel(model);
        return ResponseEntity.ok(cars);
    }

    /**
     * Lấy xe theo hãng
     * GET /api/cars/brand/{brandName}
     */
    @GetMapping("/brand/{brandName}")
    public ResponseEntity<List<CarDTO>> getCarsByBrand(@PathVariable String brandName) {
        List<CarDTO> cars = carService.getCarsByBrand(brandName);
        return ResponseEntity.ok(cars);
    }

    /**
     * Lấy xe theo khoảng giá
     * GET /api/cars/price?min=1000000&max=5000000
     */
    @GetMapping("/price")
    public ResponseEntity<List<CarDTO>> getCarsByPriceRange(
            @RequestParam double min,
            @RequestParam double max) {
        List<CarDTO> cars = carService.getCarsByPriceRange(min, max);
        return ResponseEntity.ok(cars);
    }

    /**
     * Lấy xe theo giá sau khi giảm
     * GET /api/cars/final-price?min=1000000&max=5000000
     */
    @GetMapping("/final-price")
    public ResponseEntity<List<CarDTO>> getCarsByFinalPriceRange(
            @RequestParam double min,
            @RequestParam double max) {
        List<CarDTO> cars = carService.getCarsByFinalPriceRange(min, max);
        return ResponseEntity.ok(cars);
    }

    /**
     * Lấy xe theo năm sản xuất
     * GET /api/cars/year/{year}
     */
    @GetMapping("/year/{year}")
    public ResponseEntity<List<CarDTO>> getCarsByYear(@PathVariable int year) {
        List<CarDTO> cars = carService.getCarsByYear(year);
        return ResponseEntity.ok(cars);
    }

    /**
     * Lấy xe còn hàng
     * GET /api/cars/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<CarDTO>> getAvailableCars() {
        List<CarDTO> cars = carService.getAvailableCars();
        return ResponseEntity.ok(cars);
    }


    /**
     * Tìm kiếm xe nâng cao với nhiều tiêu chí
     * GET /api/cars/advanced-search?brand=Toyota&minPrice=1000000&maxPrice=5000000
     */
    @GetMapping("/advanced-search")
    public ResponseEntity<List<CarDTO>> advancedSearch(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {
        List<CarDTO> cars = carService.searchCars(brand,year, minPrice, maxPrice);
        return ResponseEntity.ok(cars);
    }

    /**
     * Kiểm tra xe có còn trong kho không
     * GET /api/cars/{id}/check-stock?quantity=1
     */
    @GetMapping("/{id}/check-stock")
    public ResponseEntity<Boolean> checkStock(
            @PathVariable Long id,
            @RequestParam int quantity) {
        boolean available = carService.isCarAvailable(id, quantity);
        return ResponseEntity.ok(available);
    }
    // Lay cac xe nguoi dung dang ban
    @GetMapping("/car-pass")
    public ResponseEntity<List<CarDTO>> getCarPass(){
        return ResponseEntity.ok(carService.getCarsByCurrentUser());
    }
    // chinh sua thong tin xe 
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCar(
        @PathVariable Long id,
        @RequestBody CarDTO carDTO){
            carService.updateCar(id,carDTO);
            return ResponseEntity.ok().build();
    }
}
