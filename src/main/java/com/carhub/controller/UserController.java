package com.carhub.controller;

import com.carhub.dto.AppPrincipal;
import com.carhub.dto.UserDTO;
import com.carhub.entity.User;
import com.carhub.service.authentication.UserService;
import com.carhub.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    /**
     * Lấy thông tin user theo ID
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/me")
    @ResponseBody
    public ResponseEntity<?> getCurrentUserId(Authentication authentication) {
        return ResponseEntity.ok(userService.getCurrentUser(authentication).getId());
    }
    @GetMapping("/me/profile")
        public ResponseEntity<UserDTO> getCurrentUserProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getCurrentUser(authentication));
    }
    /**
     * Lấy thông tin user theo username
     * GET /api/users/username/{username}
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Tìm kiếm user theo tên
     * GET /api/users/search?name=xyz
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserDTO>> searchUserByName(@RequestParam String name) {
        List<UserDTO> users = userService.searchUserByName(name);
        return ResponseEntity.ok(users);
    }

    /**
     * Lấy tất cả user theo role
     * GET /api/users/role/{role}
     */
    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserDTO>> getUsersByRole(@PathVariable String role) {
        try {
            User.Role userRole = User.Role.valueOf(role.toUpperCase());
            List<UserDTO> users = userService.getUsersByRole(userRole);
            return ResponseEntity.ok(users);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Cập nhật thông tin user
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id,
            @RequestBody UserDTO userDTO) {
        return userService.updateUser(id, userDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Đổi mật khẩu
     * PUT /api/users/{id}/change-password
     */
    @PutMapping("/{id}/change-password")
    public ResponseEntity<String> changePassword(
            @PathVariable Long id,
            @RequestBody ChangePasswordRequest request) {
        boolean success = userService.changePassword(
                id,
                request.getOldPassword(),
                request.getNewPassword()
        );
        if (success) {
            return ResponseEntity.ok("Đổi mật khẩu thành công");
        }
        return ResponseEntity.badRequest().body("Mật khẩu cũ không chính xác");
    }

    /**
     * Xóa user
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        boolean success = userService.deleteUser(id);
        if (success) {
            return ResponseEntity.ok("Xóa user thành công");
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Kiểm tra username có tồn tại không
     * GET /api/users/check-username?username=xyz
     */
    @GetMapping("/check-username")
    public ResponseEntity<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = userService.existsByUsername(username);
        return ResponseEntity.ok(exists);
    }

    /**
     * Kiểm tra email có tồn tại không
     * GET /api/users/check-email?email=xyz@example.com
     */
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
        boolean exists = userService.existsByEmail(email);
        return ResponseEntity.ok(exists);
    }

    /**
     * DTO cho request đổi mật khẩu
     */
    @Data
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
    @PostMapping("upload-avatar")
    public ResponseEntity<?> uploadAvatar(
        @RequestParam("file") MultipartFile file,
        Authentication authentication) {

        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username).orElseThrow();

            String fileName = "avatar_" + user.getId() + "_" + file.getOriginalFilename();
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            file.transferTo(new java.io.File(uploadDir + fileName));

            // lưu DB
            user.setAvatar(fileName);
            userRepository.save(user);

            return ResponseEntity.ok(fileName);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Upload fail");
        }
    }
    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateCurrentUser(
        Authentication authentication,
        @RequestBody UserDTO userDTO)  {
            Long id = userService.getCurrentUser(authentication).getId();
            return userService.updateUser(id, userDTO)
                   .map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
        }
    
}