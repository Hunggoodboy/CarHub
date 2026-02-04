package com.fnb.carhub.repository;

import com.fnb.carhub.entity.Order;
import com.fnb.carhub.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder(Order order);
    Optional<Payment> findByOrderId(Long orderId);
    List<Payment> findByMethod(String method);
    List<Payment> findByStatus(String status);
    List<Payment> findByPaymentDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}