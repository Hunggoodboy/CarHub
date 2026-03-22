package com.carhub.dto.Response;

import com.carhub.dto.CarDTO;
import com.carhub.dto.ReviewsDTO;
import lombok.Data;

import java.util.List;

@Data
public class CarDetailResponse {
    private CarDTO car;
    private List<ReviewsDTO> reviews;
    private List<CarDTO> carsSimilar;
}
