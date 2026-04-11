package com.carhub.dto.Request;

import lombok.Data;

@Data
public class ResetPassWordRequest {
    String email;
    String otp;
    String newPassword;
}
