package com.carhub.dto;

import lombok.Data;

@Data
public class OtpRequest {
    String email;
    String otp;
}
