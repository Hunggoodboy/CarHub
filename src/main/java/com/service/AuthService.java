package com.service;

import com.dto.AuthResponse;
import com.dto.LoginRequest;
import com.dto.RegisterRequest;
import com.dto.UserDTO;
import com.entity.Customer;
import com.entity.User;
import com.repository.CustomerRepository;
import com.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        try {
            // Kiểm tra username đã tồn tại
            if (userRepository.existsByUsername(request.getUsername())) {
                return new AuthResponse(false, "Tên đăng nhập đã tồn tại", null);
            }

            // Kiểm tra email đã tồn tại
            if (userRepository.existsByEmail(request.getEmail())) {
                return new AuthResponse(false, "Email đã được sử dụng", null);
            }

            // Tạo Customer mới
            Customer customer = new Customer();
            customer.setUsername(request.getUsername());
            customer.setPassword(passwordEncoder.encode(request.getPassword()));
            customer.setFullName(request.getFullName());
            customer.setEmail(request.getEmail());
            customer.setPhoneNumber(request.getPhoneNumber());
            customer.setAddress(request.getAddress());
            customer.setShippingAddress(request.getShippingAddress());
            customer.setRole(User.Role.CUSTOMER);

            // Lưu vào database
            Customer savedCustomer = customerRepository.save(customer);

            // Chuyển đổi sang DTO
            UserDTO userDTO = UserDTO.fromEntity(savedCustomer);

            return new AuthResponse(true, "Đăng ký thành công", userDTO);
        } catch (Exception e) {
            return new AuthResponse(false, "Đăng ký thất bại: " + e.getMessage(), null);
        }
    }

    public AuthResponse login(LoginRequest request) {
        try {
            // Tìm user theo username
            System.out.println(request.getUsername());
            User user = userRepository.findByUsername(request.getUsername())
                    .orElse(null);

            if (user == null) {
                return new AuthResponse(false, "Tên đăng nhập không tồn tại", null);
            }

            // Kiểm tra mật khẩu
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return new AuthResponse(false, "Mật khẩu không chính xác", null);
            }

            // Chuyển đổi sang DTO
            UserDTO userDTO = UserDTO.fromEntity(user);

            return new AuthResponse(true, "Đăng nhập thành công", userDTO);
        } catch (Exception e) {
            return new AuthResponse(false, "Đăng nhập thất bại: " + e.getMessage(), null);
        }
    }
}