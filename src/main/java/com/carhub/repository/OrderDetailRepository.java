package com.carhub.repository;

import com.carhub.entity.Car;
import com.carhub.entity.Order;
import com.carhub.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    List<OrderDetail> findByOrder(Order order);
    List<OrderDetail> findByOrderId(Long orderId);
    List<OrderDetail> findByCar(Car car);
    List<OrderDetail> findByCarId(Long carId);
}