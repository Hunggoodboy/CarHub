package com.carhub.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SellerOrderDTO {
    private Long orderId;
    private String carName;
    private Double price;
    private Long quantity;

    private String buyerName;
    private String phone;
    private String address;

    private String status;
    private String imageUrl;
}
