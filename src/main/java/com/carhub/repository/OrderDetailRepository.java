package com.carhub.repository;

import com.carhub.entity.Car;
import com.carhub.entity.Order;
import com.carhub.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    List<OrderDetail> findByOrder(Order order);

    List<OrderDetail> findByOrderId(Long orderId);

    List<OrderDetail> findByCar(Car car);

    List<OrderDetail> findByCarId(Long carId);

    boolean existsByCarId(Long carId);

    @Query("SELECT od FROM OrderDetail od WHERE od.order.customer.id = :user_id AND od.car.id = :car_id")
    Optional<OrderDetail> findOrderDetailByCarIdAndUserId(@Param("user_id") Long user_id,
                                                           @Param("car_id") Long car_id);

    @Query("SELECT od FROM OrderDetail od " +
            "WHERE od.order.customer.id = :user_id " +
            "AND od.car.id = :car_id " +
            "AND od.order.status = com.carhub.entity.Order.Status.COMPLETED")
    Optional<OrderDetail> findCompletedOrderDetailByCarIdAndUserId(@Param("user_id") Long user_id,
                                                                    @Param("car_id") Long car_id);

    @Query("SELECT DISTINCT od.car FROM OrderDetail od WHERE od.order.customer.id = :user_id")
    List<Car> findPurchasedCarsByUserId(@Param("user_id") Long user_id);
}
