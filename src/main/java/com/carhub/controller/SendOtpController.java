package com.carhub.controller;

import com.carhub.dto.Request.EmailRequest;
import com.carhub.dto.Request.ResetPassWordRequest;
import com.carhub.service.authentication.OtpTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/password")
public class SendOtpController {
    private final OtpTokenService otpTokenService;
    @PostMapping("/RequestOTP")
    public ResponseEntity<?> requestOtp(@RequestBody EmailRequest email) {
        try {
            otpTokenService.sendOtpToken(email.getEmail());
            return ResponseEntity.ok("đã gửi otp đến email " + email.getEmail() + "Vui lòng kiểm tra");
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody ResetPassWordRequest request) {
        if (!otpTokenService.validateOtpToken(request.getOtp(), request.getEmail())) {
            return ResponseEntity.badRequest().body("Mã OTP không hợp lệ hoặc đã hết hạn");
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPassWordRequest request) {
        if (request.getOtp() == null) {
            return ResponseEntity.badRequest().body("Bạn chưa gửi mã OTP");
        }
        else if(request.getEmail() == null){
            return ResponseEntity.badRequest().body("Bạn chưa nhập Email");
        }
        System.out.println(request.getOtp());
        System.out.println(request.getEmail());
        System.out.println(request.getNewPassword());
        if(otpTokenService.validateOtpToken( request.getOtp(), request.getEmail())) {
            return ResponseEntity.ok(otpTokenService.resetPassword(request.getEmail(), request.getNewPassword()));
        }
        else{
            otpTokenService.invalidateOtpToken(request.getOtp(), request.getEmail());
             return ResponseEntity.badRequest().body("Mã OTP không hợp lệ hoặc đã hết hạn" );
        }
    }

}
