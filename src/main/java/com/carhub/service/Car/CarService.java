package com.carhub.service.Car;

import com.carhub.dto.CarDTO;
import com.carhub.dto.Response.CarDetailResponse;
import com.carhub.entity.Brand;
import com.carhub.entity.Car;
import com.carhub.entity.User;
import com.carhub.repository.*;
import com.carhub.service.ai.VectorStoreService;
import com.carhub.service.authentication.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final CarImagesSubRepository carImagesSubRepository;
    private final VectorStoreService vectorStoreService;
    private final ReviewService reviewService;
    private final OrderDetailRepository orderDetailRepository;
    private final UserService userService;
    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final ReviewsRepository reviewsRepository;
    private final FavoriteCarRepository favoriteCarRepository;
    // Lấy tất cả xe
    public List<CarDTO> getAllCars() {
        return carRepository.findAll()
                .stream()
                .map(CarDTO::fromEntity)
                .collect(Collectors.toList());
    }
    public List<CarDTO> getCarsExcludeSeller(Long sellerId) {
        return carRepository.findCarsExcludeSeller(sellerId)
            .stream()
            .map(CarDTO::fromEntity)
            .toList();
    }
    // Lấy xe mà người đang đăng nhập bán
    public List<CarDTO> getCarsByCurrentUser(){
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId= userService.getId(authentication);
        return carRepository.findBySellerId(userId)
               .stream()
               .map(CarDTO::fromEntity)
               .toList();
    }
    // Lấy thông tin xe theo ID
    public CarDTO getCarById(Long id) {
        CarDTO car = carRepository.findById(id)
                .map(CarDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + id));
        car.setSubImageUrls(carImagesSubRepository.findAllImageUrlsByCarId(id));
        return car;
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

    // Lấy tất cả xe mà người dùng hiện tại đã mua (không lọc theo trạng thái đơn hàng)
    public List<CarDTO> getPurchasedCarsForCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = userService.getId(authentication);
        return orderDetailRepository.findPurchasedCarsByUserId(userId)
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

    public List<CarDTO> getCarsForAdmin(String keyword, Long brandId) {
        return carRepository.searchForAdmin(normalize(keyword), brandId)
                .stream()
                .map(CarDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<Brand> getBrandsForAdmin() {
        return brandRepository.findAll()
                .stream()
                .filter(brand -> brand.getName() != null)
                .sorted(Comparator.comparing(Brand::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public CarDTO updateCarForAdmin(Long id, CarDTO carDTO) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y xe."));

        String model = normalize(carDTO.getModel());
        String color = normalize(carDTO.getColor());
        String description = normalize(carDTO.getDescription());

        if (model == null) {
            throw new IllegalStateException("Model xe khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng.");
        }
        if (color == null) {
            throw new IllegalStateException("MÃ u xe khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng.");
        }
        if (carDTO.getBrandId() == null) {
            throw new IllegalStateException("Vui lÃ²ng chá»n hÃ£ng xe.");
        }
        if (carDTO.getPrice() < 0) {
            throw new IllegalStateException("GiÃ¡ xe khÃ´ng há»£p lá»‡.");
        }
        if (carDTO.getDiscount() < 0 || carDTO.getDiscount() > 100) {
            throw new IllegalStateException("Giáº£m giÃ¡ pháº£i náº±m trong khoáº£ng 0-100.");
        }
        if (carDTO.getManufactureYear() <= 0) {
            throw new IllegalStateException("NÄƒm sáº£n xuáº¥t khÃ´ng há»£p lá»‡.");
        }
        if (carDTO.getStockQuantity() < 0) {
            throw new IllegalStateException("Tá»“n kho khÃ´ng Ä‘Æ°á»£c Ã¢m.");
        }

        Brand brand = brandRepository.findById(carDTO.getBrandId())
                .orElseThrow(() -> new IllegalStateException("KhÃ´ng tÃ¬m tháº¥y hÃ£ng xe."));

        car.setModel(model);
        car.setName(model);
        car.setColor(color);
        car.setDescription(description);
        car.setPrice(carDTO.getPrice());
        car.setDiscount(carDTO.getDiscount());
        car.setManufactureYear(carDTO.getManufactureYear());
        car.setStockQuantity(carDTO.getStockQuantity());
        car.setBrand(brand);

        return CarDTO.fromEntity(carRepository.save(car));
    }

    @Transactional
    public void deleteCarForAdmin(Long carId) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y xe."));

        if (orderDetailRepository.existsByCarId(carId)
                || cartItemRepository.existsByCarId(carId)
                || reviewsRepository.existsByCarId(carId)
                || favoriteCarRepository.existsByCarId(carId)) {
            throw new IllegalStateException("KhÃ´ng thá»ƒ xÃ³a xe vÃ¬ váº«n cÃ²n dá»¯ liá»‡u liÃªn quan.");
        }

        carRepository.delete(car);
    }

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
    public void saveCarService(String model, Long price, int manufactureYear, String color, String description, MultipartFile imageFile) throws IOException {
        String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
        String filePath = Paths.get("src/main/resources/static/car-images/", fileName).toString();
        Files.copy(imageFile.getInputStream(), Path.of(filePath));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long sellerId = userService.getId(authentication);
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Car car = new Car();
        car.setName(normalize(model));
        car.setModel(normalize(model));
        car.setManufactureYear(manufactureYear);
        car.setPrice(price);
        car.setDescription(normalize(description));
        car.setColor(normalize(color));
        car.setImageUrl("car-images/" + fileName);
        car.setStockQuantity(1);
        car.setSeller(seller);

        Car savedCar = carRepository.save(car);
        vectorStoreService.loadCar(savedCar);
    }

    public List<CarDTO> searchByModel(String model) {
        return carRepository.findByModelContainingIgnoreCase(model)
                .stream()
                .map(CarDTO::fromEntity)
                .collect(Collectors.toList());
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
    @Transactional
    public void updateCar(Long id , CarDTO dto){
        Car car = carRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("car not found"));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = userService.getId(auth);

        if (!car.getSeller().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền sửa xe này");
        }
        car.setModel(dto.getModel());
        car.setPrice(dto.getPrice());
        car.setColor(dto.getColor());
        car.setManufactureYear(dto.getManufactureYear());
        car.setDescription(dto.getDescription());

        carRepository.save(car);
    }
}
