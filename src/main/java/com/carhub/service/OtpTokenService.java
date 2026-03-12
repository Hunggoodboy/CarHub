package com.carhub.service;

import com.carhub.entity.OtpToken;
import com.carhub.entity.User;
import com.carhub.repository.OtpTokenRepository;
import com.carhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpTokenService {
    private  final OtpTokenRepository otpTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender javaMailSender;
    private final ResetTokenService resetTokenService;
    public String generateOtp(){
        return String.valueOf(10000 + new Random().nextInt(10000));
    }
    public void sendOtpToken(String email) {
        if(!userRepository.existsByEmail(email)){
            throw new RuntimeException("Bạn chưa tạo Email");
        }
        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(email);
        otpToken.setUsed(false);
        otpToken.setExpiryDate(LocalDateTime.now().plusMinutes(5));
        String Otp = generateOtp();
        String OtpHash = passwordEncoder.encode(Otp);
        otpToken.setOtp(OtpHash);
        otpTokenRepository.save(otpToken);
        SimpleMailMessage  message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Mã OTP để lấy lại mật khẩu của bạn");
        message.setText("Mã OTP của bạn là : " + Otp + "\n Mã này được sử dụng trong vòng 5 phút, vui lòng không được chia sẻ với bất kì ai");
        String newToken = resetTokenService.generateToken(email);
        javaMailSender.send(message);
    }
    public boolean verifyOtpToken(String Email, String otp) {
        OtpToken otpToken = otpTokenRepository.findTopByEmailOrderByExpiryDate(Email).orElseThrow(() -> new RuntimeException("Không tìm thấy OTP"));
        if(otpToken.isUsed()){
            throw new RuntimeException("OTP này đã được sử dụng");
        }
        else if (otpToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Thời gian sử dụng OTP đã quá 5 phút");
        }
        else if (!passwordEncoder.matches(otp, otpToken.getOtp())) {
            throw new RuntimeException("OTP không chính xác");
        }
        otpToken.setUsed(true);
        otpTokenRepository.save(otpToken);
        return true;
    }

    public void setPasswordEncoder(String email, String newPassword) {
        User user =  userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy mail"));
        userRepository.updateNewPassword(email, newPassword);
    }

    //Xoá mã OTP sau mỗi 5p
    @Transactional
    @Scheduled(fixedRate = 1000)
    public void deleteOTP(){
        otpTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }
}
