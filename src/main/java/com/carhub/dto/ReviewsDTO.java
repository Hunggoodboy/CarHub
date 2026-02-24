package com.carhub.dto;

import com.carhub.entity.Reviews;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewsDTO {
    private Long userId;
    private Long carId;
    private Long rating;
    private String comment;
    private LocalDateTime createdAt;
    public static ReviewsDTO fromEntity(Reviews reviews) {
        ReviewsDTO dto = new ReviewsDTO();
        dto.setUserId(reviews.getCar().getId());
        dto.setCarId(reviews.getCar().getId());
        dto.setRating(reviews.getRating());
        dto.setComment(reviews.getComment());
        dto.setCreatedAt(reviews.getCreatedAt());
        return dto;
    }
}
