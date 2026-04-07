package com.carhub.service;

import com.carhub.dto.Request.OrderRequest;
import com.carhub.entity.Car;
import com.carhub.entity.Customer;
import com.carhub.entity.Order;
import com.carhub.entity.OrderDetail;
import com.carhub.entity.Payment;
import com.carhub.entity.User;
import com.carhub.repository.CarRepository;
import com.carhub.repository.CustomerRepository;
import com.carhub.repository.OrderDetailRepository;
import com.carhub.repository.OrderRepository;
import com.carhub.repository.PaymentRepository;
import com.carhub.service.authentication.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final CarRepository carRepository;
    private final PaymentRepository paymentRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public void save(OrderRequest orderRequest) {
        validateOrderRequest(orderRequest);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long buyerId = userService.getId(authentication);
        Customer customer = customerRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("Tai khoan hien tai khong hop le de dat hang."));

        Car car = carRepository.findByIdForUpdate(orderRequest.getCarId())
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay xe."));

        if (car.getSeller() == null) {
            throw new IllegalStateException("Xe chua co nguoi ban.");
        }

        int quantity = orderRequest.getQuantity();
        if (car.getStockQuantity() < quantity) {
            throw new IllegalStateException("So luong ton kho khong du.");
        }

        String street = normalize(orderRequest.getStreet());
        String ward = normalize(orderRequest.getWard());
        String city = normalize(orderRequest.getCity());
        String deliveryAddress = resolveDeliveryAddress(orderRequest, street, ward, city);

        double priceOriginal = car.getPrice();
        double pricePaid = calculatePricePaid(car);
        double totalAmountOriginal = priceOriginal * quantity;
        double totalAmountFinal = pricePaid * quantity;
        double totalDiscount = totalAmountOriginal - totalAmountFinal;

        Order order = convertOrderRequestToOrder(
                customer,
                car.getSeller(),
                orderRequest,
                street,
                ward,
                city,
                deliveryAddress,
                totalAmountOriginal,
                totalDiscount,
                totalAmountFinal
        );
        orderRepository.save(order);

        OrderDetail orderDetail = convertOrderRequestToOrderDetail(quantity, priceOriginal, pricePaid, car, order);
        orderDetailRepository.save(orderDetail);

        Payment payment = convertOrderRequestToPayment(orderRequest, order, totalAmountFinal);
        paymentRepository.save(payment);

        car.setStockQuantity(car.getStockQuantity() - quantity);
        carRepository.save(car);
    }

    public Order convertOrderRequestToOrder(Customer customer,
                                            User seller,
                                            OrderRequest orderRequest,
                                            String street,
                                            String ward,
                                            String city,
                                            String deliveryAddress,
                                            double totalAmountOriginal,
                                            double totalDiscount,
                                            double totalAmountFinal) {
        Order order = new Order();
        order.setBuyerId(customer.getId());
        order.setSeller(seller);
        order.setCustomer(customer);
        order.setOrderDate(LocalDateTime.now());
        order.setTotalAmountOriginal(totalAmountOriginal);
        order.setTotalDiscount(totalDiscount);
        order.setTotalAmountFinal(totalAmountFinal);
        order.setDeliveryAddress(deliveryAddress);
        order.setWard(ward);
        order.setCity(city);
        order.setStreet(street);
        order.setPhone(normalize(orderRequest.getPhone()));
        order.setStatus(Order.Status.PENDING);
        return order;
    }

    public OrderDetail convertOrderRequestToOrderDetail(int quantity,
                                                        double priceOriginal,
                                                        double pricePaid,
                                                        Car car,
                                                        Order order) {
        OrderDetail detail = new OrderDetail();
        detail.setQuantity((long) quantity);
        detail.setPriceOriginal(priceOriginal);
        detail.setPricePaid(pricePaid);
        detail.setCar(car);
        detail.setOrder(order);
        return detail;
    }

    public Payment convertOrderRequestToPayment(OrderRequest orderRequest, Order order, double amount) {
        Payment payment = new Payment();
        payment.setStatus("PENDING");
        payment.setAmount(amount);

        String method = normalize(orderRequest.getPaymentMethod());
        Payment.TypePayment type;
        if ("COD".equalsIgnoreCase(method) || "CASH".equalsIgnoreCase(method)) {
            type = Payment.TypePayment.CAST;
        } else if ("BANK".equalsIgnoreCase(method) || "TRANSFER".equalsIgnoreCase(method)) {
            type = Payment.TypePayment.TRANSFER;
        } else {
            throw new IllegalArgumentException("Phuong thuc thanh toan khong hop le.");
        }

        payment.setOrder(order);
        payment.setTypePayment(type);
        payment.setPaymentDate(LocalDateTime.now());
        return payment;
    }

    private void validateOrderRequest(OrderRequest orderRequest) {
        if (orderRequest == null) {
            throw new IllegalArgumentException("Yeu cau dat hang khong hop le.");
        }
        if (orderRequest.getCarId() == null) {
            throw new IllegalArgumentException("Vui long chon xe can dat.");
        }
        if (orderRequest.getQuantity() < 1) {
            throw new IllegalArgumentException("So luong phai lon hon hoac bang 1.");
        }
        if (normalize(orderRequest.getPhone()) == null) {
            throw new IllegalArgumentException("So dien thoai khong duoc de trong.");
        }
        if (normalize(orderRequest.getPaymentMethod()) == null) {
            throw new IllegalArgumentException("Phuong thuc thanh toan khong duoc de trong.");
        }
    }

    private String resolveDeliveryAddress(OrderRequest orderRequest, String street, String ward, String city) {
        if (street != null || ward != null || city != null) {
            if (street == null || ward == null || city == null) {
                throw new IllegalArgumentException("Dia chi giao hang khong hop le.");
            }
            return String.join(", ", buildAddressParts(street, ward, city));
        }

        String deliveryAddress = normalize(orderRequest.getDeliveryAddress());
        if (deliveryAddress == null) {
            throw new IllegalArgumentException("Dia chi giao hang khong hop le.");
        }
        return deliveryAddress;
    }

    private List<String> buildAddressParts(String street, String ward, String city) {
        List<String> parts = new ArrayList<>();
        parts.add(street);
        parts.add(ward);
        parts.add(city);
        return parts;
    }

    private double calculatePricePaid(Car car) {
        return car.getPrice() * (1 - (car.getDiscount() / 100.0));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
