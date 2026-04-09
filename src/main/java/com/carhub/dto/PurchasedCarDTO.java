package com.carhub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PurchasedCarDTO {
    private Long OrderId;
    private Long CarId;
    private String model;
    private Double price;
    private String imageUrl;
    private String status; 
}