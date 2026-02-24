package com.carhub.dto;

import lombok.Data;

import java.util.List;

@Data
public class CarDetailResponse {
    private CarDTO car;
    private List<ReviewsDTO> reviews;
}
