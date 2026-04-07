package com.carhub.service.authentication;

import com.carhub.dto.UserDTO;
import com.carhub.entity.User;
import com.carhub.repository.CarRepository;
import com.carhub.repository.CartRepository;
import com.carhub.repository.ChatMessageRepository;
import com.carhub.repository.ConsultationRequestRepository;
import com.carhub.repository.FavoriteCarRepository;
import com.carhub.repository.OrderRepository;
import com.carhub.repository.ReviewsRepository;
import com.carhub.repository.UserRepository;
import com.carhub.repository.WarrantyTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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
    private final CarRepository carRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final WarrantyTicketRepository warrantyTicketRepository;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final FavoriteCarRepository favoriteCarRepository;
    private final ReviewsRepository reviewsRepository;
    private final ChatMessageRepository chatMessageRepository;

    public UserDTO getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Báº¡n chÆ°a Ä‘Äƒng nháº­p!");
        }
        if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            String email = oAuth2User.getAttribute("email");
            return getUserByEmail(email).orElseThrow(() -> new RuntimeException("KhÃ´ng tÃ¬m tháº¥y Ä‘á»‹a chá»‰ email"));
        } else if (authentication.getPrincipal() instanceof UserDetails) {
            String username = authentication.getName();
            return getUserByUsername(username).orElseThrow(() -> new RuntimeException("Báº¡n chÆ°a Ä‘Äƒng nháº­p!"));
        }
        throw new RuntimeException("NgÆ°á»i dÃ¹ng chÆ°a Ä‘Äƒng nháº­p");
    }

    public Long getId(Authentication authentication) {
        return getCurrentUser(authentication).getId();
    }

    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserDTO::fromEntity);
    }

    public Optional<UserDTO> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(UserDTO::fromEntity);
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

    @Transactional
    public Optional<UserDTO> updateUser(Long id, UserDTO userDTO) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        mergeEditableFields(user, userDTO);
        User updatedUser = userRepository.save(user);
        return Optional.of(UserDTO.fromEntity(updatedUser));
    }

    @Transactional
    public UserDTO updateUserForAdmin(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y user."));

        applyEditableFields(user, userDTO);
        return UserDTO.fromEntity(userRepository.save(user));
    }

    @Transactional
    public void deleteCustomerByAdmin(Long adminId, Long targetUserId) {
        if (Objects.equals(adminId, targetUserId)) {
            throw new IllegalStateException("KhÃ´ng thá»ƒ tá»± xÃ³a tÃ i khoáº£n cá»§a chÃ­nh mÃ¬nh.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y user."));

        if (targetUser.getRole() != User.Role.CUSTOMER) {
            throw new IllegalStateException("Chá»‰ Ä‘Æ°á»£c xÃ³a tÃ i khoáº£n CUSTOMER.");
        }

        if (hasRelatedData(targetUserId)) {
            throw new IllegalStateException("KhÃ´ng thá»ƒ xÃ³a user vÃ¬ váº«n cÃ²n dá»¯ liá»‡u liÃªn quan.");
        }

        userRepository.delete(targetUser);
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

    @Transactional
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    private void applyEditableFields(User user, UserDTO userDTO) {
        String fullName = normalize(userDTO.getFullName());
        String email = normalize(userDTO.getEmail());
        String phoneNumber = normalize(userDTO.getPhoneNumber());
        String address = normalize(userDTO.getAddress());

        if (email != null && !email.equalsIgnoreCase(user.getEmail()) && existsByEmail(email)) {
            throw new IllegalStateException("Email Ä‘Ã£ Ä‘Æ°á»£c sá»­ dá»¥ng.");
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
                throw new IllegalStateException("Email Ä‘Ã£ Ä‘Æ°á»£c sá»­ dá»¥ng.");
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
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
