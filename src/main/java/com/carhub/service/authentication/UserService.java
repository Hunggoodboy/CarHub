package com.carhub.service.authentication;

import com.carhub.dto.UserDTO;
import com.carhub.entity.Customer;
import com.carhub.entity.User;
import com.carhub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerRepository customerRepository;
    private final CarRepository carRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final WarrantyTicketRepository warrantyTicketRepository;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final FavoriteCarRepository favoriteCarRepository;
    private final ReviewsRepository reviewsRepository;
    private final ChatMessageRepository chatMessageRepository;

    // ==================== CURRENT USER ====================

    public UserDTO getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Bạn chưa đăng nhập!");
        }
        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");
            return getUserByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ email"));
        } else if (authentication.getPrincipal() instanceof UserDetails) {
            String username = authentication.getName();
            return getUserByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Bạn chưa đăng nhập!"));
        }
        throw new RuntimeException("Người dùng chưa đăng nhập");
    }

    public Long getId(Authentication authentication) {
        return getCurrentUser(authentication).getId();
    }

    public Customer getCurrentCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = getId(authentication);
        return customerRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy customer"));
    }

    // ==================== QUERY ====================

    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserDTO::fromEntity);
    }

    public Optional<UserDTO> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserDTO::fromEntity);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Optional<Long> getIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(User::getId);
    }

    public Optional<UserDTO> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserDTO::fromEntity);
    }

    public List<UserDTO> getUsersByRole(User.Role role) {
        return userRepository.findByRole(role)
                .stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<UserDTO> searchUserByName(String name) {
        return userRepository.findByFullNameContainingIgnoreCase(name)
                .stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<UserDTO> getUsersForAdmin(String keyword) {
        return userRepository.searchForAdmin(normalize(keyword))
                .stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // ==================== UPDATE ====================

    @Transactional
    public Optional<UserDTO> updateUser(Long id, UserDTO userDTO) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        mergeEditableFields(user, userDTO);
        return Optional.of(UserDTO.fromEntity(userRepository.save(user)));
    }

    @Transactional
    public UserDTO updateUserForAdmin(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user."));
        applyEditableFields(user, userDTO);
        return UserDTO.fromEntity(userRepository.save(user));
    }

    @Transactional
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(oldPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    // ==================== DELETE ====================

    @Transactional
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public void deleteCustomerByAdmin(Long adminId, Long targetUserId) {
        if (Objects.equals(adminId, targetUserId)) {
            throw new IllegalStateException("Không thể tự xóa tài khoản của chính mình.");
        }
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user."));
        if (targetUser.getRole() != User.Role.CUSTOMER) {
            throw new IllegalStateException("Chỉ được xóa tài khoản CUSTOMER.");
        }
        if (hasRelatedData(targetUserId)) {
            throw new IllegalStateException("Không thể xóa user vì vẫn còn dữ liệu liên quan.");
        }
        userRepository.delete(targetUser);
    }

    // ==================== PRIVATE HELPERS ====================

    private void applyEditableFields(User user, UserDTO userDTO) {
        String fullName = normalize(userDTO.getFullName());
        String email = normalize(userDTO.getEmail());
        String phoneNumber = normalize(userDTO.getPhoneNumber());
        String address = normalize(userDTO.getAddress());

        if (email != null && !email.equalsIgnoreCase(user.getEmail()) && existsByEmail(email)) {
            throw new IllegalStateException("Email đã được sử dụng.");
        }

        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setAddress(address);
    }

    private void mergeEditableFields(User user, UserDTO userDTO) {
        if (userDTO.getFullName() != null) {
            user.setFullName(normalize(userDTO.getFullName()));
        }
        if (userDTO.getEmail() != null) {
            String email = normalize(userDTO.getEmail());
            if (email != null && !email.equalsIgnoreCase(user.getEmail()) && existsByEmail(email)) {
                throw new IllegalStateException("Email đã được sử dụng.");
            }
            user.setEmail(email);
        }
        if (userDTO.getPhoneNumber() != null) {
            user.setPhoneNumber(normalize(userDTO.getPhoneNumber()));
        }
        if (userDTO.getAddress() != null) {
            user.setAddress(normalize(userDTO.getAddress()));
        }
    }

    private boolean hasRelatedData(Long userId) {
        return carRepository.existsBySellerId(userId)
                || cartRepository.existsByCustomerId(userId)
                || orderRepository.existsByCustomerId(userId)
                || warrantyTicketRepository.existsByCustomerId(userId)
                || consultationRequestRepository.existsByCustomerId(userId)
                || favoriteCarRepository.existsByUserId(userId)
                || reviewsRepository.existsByUserId(userId)
                || chatMessageRepository.existsBySenderIdOrReceiverId(userId, userId);
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}