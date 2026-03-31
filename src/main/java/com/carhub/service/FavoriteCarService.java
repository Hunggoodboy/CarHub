package com.carhub.service;

import com.carhub.dto.CarDTO;
import com.carhub.entity.Car;
import com.carhub.entity.FavoriteCar;
import com.carhub.entity.User;
import com.carhub.repository.CarRepository;
import com.carhub.repository.FavoriteCarRepository;
import com.carhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteCarService {

    private final FavoriteCarRepository favoriteCarRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    /** Lấy danh sách xe yêu thích của user hiện tại */
    public List<CarDTO> getFavoriteCars() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = userService.getId(authentication);
        return favoriteCarRepository.findFavoriteCarsByUserId(userId)
                .stream()
                .map(CarDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /** Thêm xe vào yêu thích. Trả về true nếu thêm mới, false nếu đã tồn tại */
    @Transactional
    public boolean addFavorite(Long carId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = userService.getId(authentication);

        if (favoriteCarRepository.existsByUserIdAndCarId(userId, carId)) {
            return false; // đã có rồi
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe"));

        FavoriteCar favorite = new FavoriteCar();
        favorite.setUser(user);
        favorite.setCar(car);
        favoriteCarRepository.save(favorite);
        return true;
    }

    /** Xóa xe khỏi yêu thích */
    @Transactional
    public void removeFavorite(Long carId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = userService.getId(authentication);
        favoriteCarRepository.deleteByUserIdAndCarId(userId, carId);
    }

    /** Toggle: nếu chưa có thì thêm, đã có thì xóa. Trả về trạng thái mới */
    @Transactional
    public boolean toggleFavorite(Long carId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = userService.getId(authentication);

        if (favoriteCarRepository.existsByUserIdAndCarId(userId, carId)) {
            favoriteCarRepository.deleteByUserIdAndCarId(userId, carId);
            return false; // đã xóa
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
            Car car = carRepository.findById(carId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy xe"));
            FavoriteCar favorite = new FavoriteCar();
            favorite.setUser(user);
            favorite.setCar(car);
            favoriteCarRepository.save(favorite);
            return true; // đã thêm
        }
    }

    /** Kiểm tra xe có trong yêu thích không */
    public boolean isFavorite(Long carId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = userService.getId(authentication);
        return favoriteCarRepository.existsByUserIdAndCarId(userId, carId);
    }

    /** Lấy danh sách id xe yêu thích (dùng cho frontend biết xe nào đã thích) */
    public List<Long> getFavoriteCarIds() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = userService.getId(authentication);
        return favoriteCarRepository.findFavoriteCarsByUserId(userId)
                .stream()
                .map(Car::getId)
                .collect(Collectors.toList());
    }
}