package com.carhub.dto;

import lombok.Data;

@Data
public class OrderRequest {
    private Long carId;
    private String street;
    private String ward;
    private String city;
    private String phone;
    private String paymentMethod;
}

