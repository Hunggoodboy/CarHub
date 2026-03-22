package com.carhub.dto.Request;

import lombok.Data;

@Data
public class OrderRequest {
    private Long carId;
    private String DeliveryAddress;
    private String street;
    private String ward;
    private String city;
    private String phone;
    private String paymentMethod;
    private int quantity;
    private Double priceOriginal;
    private Double pricePaid;
    private Double discount;
    private Double totalAmountOriginal;
    private Double totalAmountFinal;
    private Double totalDiscount;
}
