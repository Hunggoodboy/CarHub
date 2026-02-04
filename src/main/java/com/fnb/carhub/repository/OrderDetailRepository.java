package com.fnb.carhub.repository;

import com.fnb.carhub.entity.Car;
import com.fnb.carhub.entity.Order;
import com.fnb.carhub.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    List<OrderDetail> findByOrder(Order order);
    List<OrderDetail> findByOrderId(Long orderId);
    List<OrderDetail> findByCar(Car car);
    List<OrderDetail> findByCarId(Long carId);
}