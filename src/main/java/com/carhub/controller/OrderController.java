package com.carhub.controller;

import com.carhub.dto.Request.OrderRequest;
import com.carhub.service.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/orders")
@AllArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<?> addOrder(@RequestBody OrderRequest orderRequest) {
        try {
            orderService.save(orderRequest);
            return ResponseEntity.ok(Map.of("message", "Dat hang thanh cong"));
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
        } catch (NoSuchElementException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage());
        } catch (IllegalStateException e) {
            return buildErrorResponse(HttpStatus.CONFLICT, "CONFLICT", e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Dat hang that bai.");
        }
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
