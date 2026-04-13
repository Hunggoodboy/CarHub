package com.carhub.dto;
 
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PurchasedCarDTO {
    private Long orderId;
    private Long carId;
    private String model;
    private Double price;
    private String imageUrl;
    private String status; 
    private LocalDateTime orderDate;
}