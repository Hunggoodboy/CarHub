package com.carhub.controller;

import com.carhub.dto.EmailRequest;
import com.carhub.dto.OtpRequest;
import com.carhub.dto.ResetPasswordRequest;
import com.carhub.repository.UserRepository;
import com.carhub.service.OtpTokenService;
import com.carhub.service.ResetTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/password")
public class SendOtpController {
    private final OtpTokenService otpTokenService;
    private final ResetTokenService resetTokenService;
    private final UserRepository userRepository;
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

    @PostMapping("/verifyOtp")
    public ResponseEntity<?> sendOtp(@RequestBody OtpRequest otpRequest) {
        if (otpRequest.getOtp() == null) {
            return ResponseEntity.badRequest().body("Bạn chưa gửi mã OTP");
        }
        else if(otpRequest.getEmail() == null){
            return ResponseEntity.badRequest().body("Bạn chưa nhập Email");
        }
        else if(otpTokenService.verifyOtpToken(otpRequest.getEmail(), otpRequest.getOtp())) {
            String resetToken = resetTokenService.generateToken(otpRequest.getEmail());
            return ResponseEntity.ok(Map.of(
                    "message", "OTP hợp lệ!",
                    "resetToken", resetToken  // ← Frontend nhận, lưu lại
            ));
        }
        return ResponseEntity.badRequest().body("Lỗi");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest resetPasswordRequest) {
            String newPassword = resetPasswordRequest.getNewPassword();
            otpTokenService.setPasswordEncoder(newPassword, resetPasswordRequest.getEmail());
            return ResponseEntity.ok().build();
    }
}
