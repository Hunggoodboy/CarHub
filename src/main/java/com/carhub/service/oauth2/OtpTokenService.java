package com.carhub.service.oauth2;

import com.carhub.entity.OtpToken;
import com.carhub.entity.User;
import com.carhub.repository.OtpTokenRepository;
import com.carhub.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class OtpTokenService {
    private final OtpTokenRepository repo;
    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String generateOtpToken(String email) {
        repo.findFirstByEmailOrderByCreatedAtDesc(email).ifPresent(last -> {
            if(!last.getCreatedAt().plusMinutes(1).isBefore(LocalDateTime.now())) {
                long secondLeft = Duration.between(LocalDateTime.now(), last.getCreatedAt().plusMinutes(1)).getSeconds();
                throw new RuntimeException("Vui lòng chờ " + secondLeft + " giây trước khi gửi lại OTP");
            }
        });
        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(email);
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        otpToken.setOtp(passwordEncoder.encode(otp));
        otpToken.setCreatedAt(LocalDateTime.now());
        otpToken.setExpiryAt(LocalDateTime.now().plusMinutes(5));
        otpToken.setCountUsed(0);
        repo.save(otpToken);
        return otp;
    }
    public void sendOtpToken(String email) {
        if(!userRepository.existsByEmail(email)){
            throw new RuntimeException("Bạn chưa tạo Email");
        }
        String otpToken = generateOtpToken(email);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Mã OTP để lấy lại mật khẩu của bạn");
        message.setText("Mã OTP của bạn là : " + otpToken + "\n Mã này được sử dụng trong vòng 5 phút, vui lòng không được chia sẻ với bất kì ai");
        javaMailSender.send(message);
    }

    public boolean validateOtpToken(String otpToken, String email) {
        OtpToken otp = repo.findFirstByEmailOrderByCreatedAtDesc(email).orElse(null);
        if (otp == null) {
            System.out.println("❌ Không tìm thấy OTP cho email: " + email);
            return false;
        }

        System.out.println("OTP trong DB: " + otp.getOtp());
        System.out.println("OTP nhận vào: " + otpToken);
        System.out.println("Hết hạn lúc: " + otp.getExpiryAt());
        System.out.println("Thời gian hiện tại: " + LocalDateTime.now());
        System.out.println("countUsed: " + otp.getCountUsed());
        return passwordEncoder.matches(otpToken, otp.getOtp()) && LocalDateTime.now().isBefore(otp.getExpiryAt()) && otp.getCountUsed() <= 5;
    }

    public void invalidateOtpToken(String otpToken, String email) {
        repo.findFirstByEmailOrderByCreatedAtDesc(email).ifPresent(otp -> {
            if (otp.getOtp().equals(otpToken)) {
                otp.setCountUsed(otp.getCountUsed() + 1);
                repo.save(otp);
            }
        });
    }

    public String resetPassword(String email, String newPassword) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return "Đã reset mật khẩu thành công cho email " + email;
        }
        catch (Exception e) {
            return e.getMessage();
        }
    }

}
