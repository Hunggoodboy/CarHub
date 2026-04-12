package com.carhub.service;

import com.carhub.dto.UserDTO;
import com.carhub.entity.Customer;
import com.carhub.entity.User;
import com.carhub.repository.CustomerRepository;
import com.carhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerRepository customerRepository;

    // Lấy id của người dùng hiện tại
    public UserDTO getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Bạn chưa đăng nhập!");
        }
        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");
            return getUserByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ email"));
        } else if (authentication.getPrincipal() instanceof UserDetails) {
            String username = authentication.getName();
            return getUserByUsername(username).orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập!"));
        }
        throw new RuntimeException("Người dùng chưa đăng nhập");
    }

    public Long getId(Authentication authentication) {
        return getCurrentUser(authentication).getId();
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Lấy thông tin user theo ID
    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserDTO::fromEntity);
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Lấy thông tin user theo username
    public Optional<UserDTO> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserDTO::fromEntity);
    }

    public Optional<Long> getIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(User::getId);
    }

    // Lấy thông tin user theo email
    public Optional<UserDTO> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserDTO::fromEntity);
    }

    // Lấy tất cả user theo role
    public List<UserDTO> getUsersByRole(User.Role role) {
        return userRepository.findByRole(role)
                .stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Tìm kiếm user theo tên
    public List<UserDTO> searchUserByName(String name) {
        return userRepository.findByFullNameContainingIgnoreCase(name)
                .stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // Cập nhật thông tin user
    @Transactional
    public Optional<UserDTO> updateUser(Long id, UserDTO userDTO) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (userDTO.getFullName() != null) {
                user.setFullName(userDTO.getFullName());
            }
            if (userDTO.getEmail() != null) {
                user.setEmail(userDTO.getEmail());
            }
            if (userDTO.getPhoneNumber() != null) {
                user.setPhoneNumber(userDTO.getPhoneNumber());
            }
            if (userDTO.getAddress() != null) {
                user.setAddress(userDTO.getAddress());
            }

            User updatedUser = userRepository.save(user);
            return Optional.of(UserDTO.fromEntity(updatedUser));
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<UserDTO> updateUserByAdmin(Long id, UserDTO userDTO) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();

        if (userDTO.getFullName() != null) {
            user.setFullName(userDTO.getFullName());
        }
        if (userDTO.getEmail() != null) {
            user.setEmail(userDTO.getEmail());
        }
        if (userDTO.getPhoneNumber() != null) {
            user.setPhoneNumber(userDTO.getPhoneNumber());
        }
        if (userDTO.getAddress() != null) {
            user.setAddress(userDTO.getAddress());
        }
        if (userDTO.getRole() != null && !userDTO.getRole().isBlank()) {
            user.setRole(User.Role.valueOf(userDTO.getRole().toUpperCase()));
        }

        User updatedUser = userRepository.save(user);
        return Optional.of(UserDTO.fromEntity(updatedUser));
    }

    // Đổi mật khẩu
    @Transactional
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Kiểm tra mật khẩu cũ
            if (passwordEncoder.matches(oldPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    // Xóa user
    @Transactional
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deleteUserAsAdmin(Long id, Authentication authentication) {
        Long currentUserId = getId(authentication);
        if (currentUserId != null && currentUserId.equals(id)) {
            throw new RuntimeException("Bạn không thể xóa chính tài khoản của mình");
        }

        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Kiểm tra username có tồn tại không
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    // Kiểm tra email có tồn tại không
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Customer getCurrentCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return customerRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy customer"));
    }
}