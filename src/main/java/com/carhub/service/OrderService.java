package com.carhub.service;

import com.carhub.dto.OrderRequest;
import com.carhub.entity.Car;
import com.carhub.entity.Order;
import com.carhub.entity.OrderDetail;
import com.carhub.entity.Payment;
import com.carhub.repository.CarRepository;
import com.carhub.repository.OrderDetailRepository;
import com.carhub.repository.OrderRepository;
import com.carhub.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final CarRepository carRepository;
    private final PaymentRepository paymentRepository;
    private final OrderDetailRepository orderDetailRepository;
    public void save(OrderRequest orderRequest) {
        Car car = carRepository.findById(orderRequest.getCarId()).get();
        Order order = convertOrderRequestToOrder(orderRequest);
        orderRepository.save(order);
        OrderDetail orderDetail = convertOrderRequestToOrderDetail(orderRequest, car,  order);
        orderDetailRepository.save(orderDetail);
        Payment payment = convertOrderRequestToPayment(orderRequest, car,  order);
        paymentRepository.save(payment);
    }
    public Order convertOrderRequestToOrder(OrderRequest orderRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long buyerId = userService.getId(authentication);
        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setSellerId(carRepository.findCustomerIdById(orderRequest.getCarId()));
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmountOriginal(orderRequest.getTotalAmountOriginal());
        order.setTotalDiscount(orderRequest.getTotalDiscount());
        order.setTotalAmountFinal(orderRequest.getTotalAmountFinal());
        order.setDeliveryAddress(orderRequest.getDeliveryAddress());
        order.setWard(orderRequest.getWard());
        order.setCity(orderRequest.getCity());
        order.setStreet(orderRequest.getStreet());
        order.setPhone(orderRequest.getPhone());
        order.setStatus(Order.Status.PENDING);
        return order;
    }
    public OrderDetail convertOrderRequestToOrderDetail(OrderRequest orderRequest, Car car, Order order) {
        OrderDetail detail = new OrderDetail();
        detail.setQuantity((long) orderRequest.getQuantity());
        detail.setPriceOriginal(orderRequest.getPriceOriginal());
        detail.setPricePaid(orderRequest.getPricePaid());
        detail.setCar(car);
        detail.setOrder(order);
        return detail;
    }
    public Payment convertOrderRequestToPayment(OrderRequest orderRequest, Car car, Order order) {
        Payment payment = new Payment();
        payment.setStatus("PENDING");
        payment.setTypePayment(Payment.TypePayment.valueOf(orderRequest.getPaymentMethod()));
        payment.setPaymentDate(LocalDateTime.now());
        return payment;
    }
}
