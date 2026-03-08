package com.carhub.controller;

import com.carhub.dto.OrderRequest;
import com.carhub.entity.Order;
import com.carhub.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody OrderRequest request,
                                                           Authentication authentication) {
        String username = authentication.getName();
        Order order = orderService.createOrder(request, username);

        Map<String, Object> body = new HashMap<>();
        body.put("orderId", order.getId());
        body.put("message", "Tạo đơn hàng thành công");

        return ResponseEntity.ok(body);
    }
}

