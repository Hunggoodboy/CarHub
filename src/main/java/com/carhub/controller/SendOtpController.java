package com.carhub.controller;

import com.carhub.dto.EmailRequest;
import com.carhub.entity.OtpToken;
import com.carhub.service.OtpTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/password")
public class SendOtpController {
    private final OtpTokenService otpTokenService;

    @PostMapping("/send-OTP")
    public ResponseEntity<?> sendOtp(@RequestBody EmailRequest email) {
        try {
            otpTokenService.sendOtpToken(email.getEmail());
            return ResponseEntity.ok("đã gửi otp đến email " + email.getEmail() + "Vui lòng kiểm tra");
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
