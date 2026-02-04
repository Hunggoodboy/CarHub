package com.fnb.carhub.dto;

import com.fnb.carhub.entity.Car;
import lombok.Data;

@Data
public class CarDTO {
    private Long id;
    private String model;
    private String color;
    private String description;
    private String imageUrl;
    private double price;
    private double discount;
    private double finalPrice;
    private int manufactureYear;
    private int stockQuantity;
    private String brandName;
    private String brandOrigin;
    private Long brandId;

    public static CarDTO fromEntity(Car car) {
        CarDTO dto = new CarDTO();
        dto.setId(car.getId());
        dto.setModel(car.getModel());
        dto.setColor(car.getColor());
        dto.setDescription(car.getDescription());
        dto.setImageUrl(car.getImageUrl());
        dto.setPrice(car.getPrice());
        dto.setDiscount(car.getDiscount());
        dto.setFinalPrice(car.getPrice() * (1 - car.getDiscount() / 100));
        dto.setManufactureYear(car.getManufactureYear());
        dto.setStockQuantity(car.getStockQuantity());

        if (car.getBrand() != null) {
            dto.setBrandId(car.getBrand().getId());
            dto.setBrandName(car.getBrand().getName());
            dto.setBrandOrigin(car.getBrand().getOrigin());
        }

        return dto;
    }
}