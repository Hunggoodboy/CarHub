package com.carhub.service;

import com.carhub.dto.OrderRequest;
import com.carhub.entity.*;
import com.carhub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final PaymentRepository paymentRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;

    @Transactional
    public Order createOrder(OrderRequest request, String username) {
        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe với id = " + request.getCarId()));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với username = " + username));

        double priceOriginal = car.getPrice();
        double priceFinal = car.getPrice() * (1 - car.getDiscount()/100);

        Order order = new Order();
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmountOriginal(priceOriginal);
        order.setTotalAmountFinal(priceFinal);
        order.setStatus("PENDING");
        order.setDeliveryAddress(
                request.getStreet() + ", " +
                request.getWard() + ", " +
                request.getCity()
        );
        order.setUser(user);

        Order savedOrder = orderRepository.save(order);

        OrderDetail detail = new OrderDetail();
        detail.setOrder(savedOrder);
        detail.setCar(car);
        detail.setQuantity(1L);
        detail.setPriceOriginal(priceOriginal);
        detail.setPricePaid(priceFinal);
        orderDetailRepository.save(detail);

        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setMethod(request.getPaymentMethod());
        payment.setStatus("PENDING");
        payment.setAmount(priceFinal);
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);

        // Giảm số lượng tồn kho (nếu có)
        if (car.getStockQuantity() > 0) {
            car.setStockQuantity(car.getStockQuantity() - 1);
            carRepository.save(car);
        }

        return savedOrder;
    }
}

