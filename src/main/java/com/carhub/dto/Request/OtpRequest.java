package com.carhub.dto.Request;

import lombok.Data;

@Data
public class OtpRequest {
    String email;
    String otp;
}
