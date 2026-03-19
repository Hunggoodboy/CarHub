package com.carhub.dto.Request;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String email;
    private String newPassword;
}
