package com.carhub.controller;

import com.carhub.dto.SellerOrderDTO;
import com.carhub.dto.Request.OrderRequest;
import com.carhub.service.OrderService;
import com.carhub.entity.Order;
import com.carhub.entity.User;
import com.carhub.service.UserService;
import com.carhub.repository.OrderRepository;

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
    private final UserService userService;
    private final OrderRepository orderRepository;

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

    @GetMapping("/purchased")
public ResponseEntity<?> getPurchasedCars(@RequestParam String status) {
    Order.Status st = Order.Status.valueOf(status.toUpperCase());
    return ResponseEntity.ok(orderService.getPurchasedCarsByStatus(st));
}

    @GetMapping("/seller/orders")
    public ResponseEntity<List<SellerOrderDTO>> getSellerOrders(
            @RequestParam String status,
            Authentication auth
    ) {
        String username = auth.getName();
        User user = userService.findByUsername(username);

        Long sellerId = user.getId();

        Order.Status st;
        try {
            st = Order.Status.valueOf(status.toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(orderService.getOrdersForSeller(sellerId, st));
    }

    @GetMapping("/my")
    public List<Order> getMyOrder(@RequestParam Order.Status status, Authentication auth){
        String username = auth.getName();
        User user = userService.findByUsername(username);

        return orderRepository.findByBuyerIdAndStatus(user.getId(), status);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status
    ){
        Order order = orderRepository.findById(id).orElseThrow();

        try {
            Order.Status newStatus = Order.Status.valueOf(status.toUpperCase());
            order.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Status không hợp lệ!");
        }

        orderRepository.save(order);

        return ResponseEntity.ok("Updated");
    }

    // Seller bắt đầu giao
    @PutMapping("/{id}/start-delivery")
    public ResponseEntity<?> startDelivery(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElseThrow();

        order.setStatus(Order.Status.DELIVERING);
        orderRepository.save(order);

        return ResponseEntity.ok("Started delivery");
    }

    //  Seller xác nhận đã giao
    @PutMapping("/{id}/delivered")
    public ResponseEntity<?> delivered(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElseThrow();

        order.setStatus(Order.Status.DELIVERED);
        orderRepository.save(order);

        return ResponseEntity.ok("Delivered");
    }

    // Buyer xác nhận
    @PutMapping("/{id}/confirm-buyer")
    public ResponseEntity<?> confirmBuyer(@PathVariable Long id) {
        orderService.confirmBuyer(id);
        return ResponseEntity.ok("Buyer confirmed");
    }

    //  Seller xác nhận
    @PutMapping("/{id}/confirm-seller")
    public ResponseEntity<?> confirmSeller(@PathVariable Long id) {
        orderService.confirmSeller(id);
        return ResponseEntity.ok("Seller confirmed");
    }
}