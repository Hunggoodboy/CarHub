package com.carhub.service;

import com.carhub.controller.UserController;
import com.carhub.dto.CarDTO;
import com.carhub.dto.ReviewsDTO;
import com.carhub.dto.UserDTO;
import com.carhub.entity.Car;
import com.carhub.entity.OrderDetail;
import com.carhub.entity.Reviews;
import com.carhub.entity.User;
import com.carhub.exception.NotPurchasedException;
import com.carhub.repository.CarRepository;
import com.carhub.repository.OrderDetailRepository;
import com.carhub.repository.ReviewsRepository;
import com.carhub.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ReviewService {

    private final ReviewsRepository reviewsRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final CarRepository carRepository;
    public List<ReviewsDTO> getReviewsByCarId(Long id) {
        return reviewsRepository.getReviewsByCarId(id)
                .stream()
                .map(ReviewsDTO::fromEntity).collect(Collectors.toList());
    }
    public boolean userBuyThisCarId(Long carId) {
        Long userId = userService.getId();
        return orderDetailRepository.findOrderDetailByCarIdAndUserId(userId, carId).isPresent();
    }

    public void createReview(ReviewsDTO reviewsDTO, Long CarId) {
        if(userBuyThisCarId(CarId)) {
            Car car = carRepository.findById(CarId).orElseThrow(() -> new RuntimeException("Car not found"));
            User user = userRepository.findById(userService.getId()).orElseThrow(() -> new RuntimeException("User not found"));
            Reviews reviews = new Reviews();
            reviews.setCreatedAt(LocalDateTime.now());
            reviews.setUser(user);
            reviews.setCar(car);
            reviews.setRating(reviewsDTO.getRating());
            reviews.setComment(reviewsDTO.getComment());
            reviewsRepository.save(reviews);
        }
        else{
            throw new NotPurchasedException("Bạn chưa mua sản phẩm");
        }
    }
}
