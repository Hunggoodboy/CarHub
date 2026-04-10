package com.carhub.dto.Request;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String confirmPassword;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String shippingAddress; // For customer
}