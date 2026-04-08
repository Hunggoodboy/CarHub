package com.carhub.service;

import com.carhub.dto.Request.OrderRequest;
import com.carhub.dto.PurchasedCarDTO;
import com.carhub.dto.SellerOrderDTO;
import com.carhub.entity.*;
import com.carhub.repository.*;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserService userService;
    private final CarRepository carRepository;
    private final PaymentRepository paymentRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CustomerRepository customerRepository;

    public void save(OrderRequest orderRequest) {
        Car car = carRepository.findById(orderRequest.getCarId()).get();

        Order order = convertOrderRequestToOrder(orderRequest);
        orderRepository.save(order);

        OrderDetail orderDetail = convertOrderRequestToOrderDetail(orderRequest, car, order);
        orderDetailRepository.save(orderDetail);

        Payment payment = convertOrderRequestToPayment(orderRequest, order);
        paymentRepository.save(payment);
    }

    public Order convertOrderRequestToOrder(OrderRequest orderRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long buyerId = userService.getId(authentication);

        Order order = new Order();
        order.setBuyerId(buyerId);

        order.setCustomer(
            customerRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"))
        );

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

        order.setBuyerConfirmed(false);
        order.setSellerConfirmed(false);

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

    public Payment convertOrderRequestToPayment(OrderRequest orderRequest, Order order) {
        Payment payment = new Payment();
        payment.setStatus("PENDING");

        String method = orderRequest.getPaymentMethod();
        Payment.TypePayment type;

        if ("COD".equalsIgnoreCase(method)) {
            type = Payment.TypePayment.CAST;
        } else {
            type = Payment.TypePayment.TRANSFER;
        }

        payment.setOrder(order);
        payment.setTypePayment(type);
        payment.setPaymentDate(LocalDateTime.now());

        return payment;
    }

    public List<SellerOrderDTO> getOrdersForSeller(Long sellerId, Order.Status status) {
        List<OrderDetail> list = orderDetailRepository.findBySellerAndStatus(sellerId, status);

        return list.stream().map(od -> {
            Order order = od.getOrder();

            return new SellerOrderDTO(
                order.getId(),
                od.getCar().getModel(),
                od.getCar().getPrice(),
                od.getQuantity(),
                order.getCustomer().getFullName(),
                order.getPhone(),
                order.getStreet(),
                order.getStatus().name(),
                od.getCar().getImageUrl()
            );
        }).toList();
    }

    public void confirmBuyer(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setBuyerConfirmed(true);

        if (order.getSellerConfirmed()) {
            order.setStatus(Order.Status.COMPLETED);
        }

        orderRepository.save(order);
    }

    public void confirmSeller(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setSellerConfirmed(true);

        //  ca 2 confirm -> Completed
        if (order.getBuyerConfirmed()) {
            order.setStatus(Order.Status.COMPLETED);
        }

        orderRepository.save(order);
    }

    public void startDelivery(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(Order.Status.DELIVERING);
        orderRepository.save(order);
    }
    public List<PurchasedCarDTO> getPurchasedCarsByStatus(Order.Status status) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Long buyerId = userService.getId(auth);

    List<Order> orders = orderRepository.findByBuyerIdAndStatus(buyerId, status);

    return orders.stream()
            .flatMap(o -> o.getOrderDetails().stream().map(od -> {
                Car car = od.getCar();

                return new PurchasedCarDTO(
                        o.getId(),
                        car.getId(),
                        car.getModel(),
                        car.getPrice(),
                        car.getImageUrl(),
                        o.getStatus().name()
                );
            }))
            .toList();
}
}