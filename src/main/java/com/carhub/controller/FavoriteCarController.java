package com.carhub.controller;

import com.carhub.dto.CarDTO;
import com.carhub.service.FavoriteCarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteCarController {

    private final FavoriteCarService favoriteCarService;

    /**
     * Lấy danh sách xe yêu thích của user đang đăng nhập
     * GET /api/favorites
     */
    @GetMapping
    public ResponseEntity<List<CarDTO>> getFavorites() {
        return ResponseEntity.ok(favoriteCarService.getFavoriteCars());
    }

    /**
     * Lấy danh sách ID xe yêu thích (để highlight nút tim trên trang list)
     * GET /api/favorites/ids
     */
    @GetMapping("/ids")
    public ResponseEntity<List<Long>> getFavoriteIds() {
        return ResponseEntity.ok(favoriteCarService.getFavoriteCarIds());
    }

    /**
     * Toggle yêu thích: thêm nếu chưa có, xóa nếu đã có
     * POST /api/favorites/{carId}/toggle
     */
    @PostMapping("/{carId}/toggle")
    public ResponseEntity<Map<String, Object>> toggleFavorite(@PathVariable Long carId) {
        boolean isFavorited = favoriteCarService.toggleFavorite(carId);
        return ResponseEntity.ok(Map.of(
                "favorited", isFavorited,
                "message", isFavorited ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích"
        ));
    }

    /**
     * Thêm xe vào yêu thích
     * POST /api/favorites/{carId}
     */
    @PostMapping("/{carId}")
    public ResponseEntity<Map<String, Object>> addFavorite(@PathVariable Long carId) {
        boolean added = favoriteCarService.addFavorite(carId);
        if (added) {
            return ResponseEntity.ok(Map.of("message", "Đã thêm vào yêu thích", "favorited", true));
        }
        return ResponseEntity.ok(Map.of("message", "Xe đã có trong danh sách yêu thích", "favorited", true));
    }

    /**
     * Xóa xe khỏi yêu thích
     * DELETE /api/favorites/{carId}
     */
    @DeleteMapping("/{carId}")
    public ResponseEntity<Map<String, Object>> removeFavorite(@PathVariable Long carId) {
        favoriteCarService.removeFavorite(carId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa khỏi yêu thích", "favorited", false));
    }

    /**
     * Kiểm tra xe có trong yêu thích không
     * GET /api/favorites/{carId}/check
     */
    @GetMapping("/{carId}/check")
    public ResponseEntity<Map<String, Boolean>> checkFavorite(@PathVariable Long carId) {
        return ResponseEntity.ok(Map.of("favorited", favoriteCarService.isFavorite(carId)));
    }
}