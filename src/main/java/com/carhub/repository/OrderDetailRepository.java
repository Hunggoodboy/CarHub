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
    //Tìm xe theo user_id và car_
    @Query("SELECT od FROM OrderDetail od WHERE od.order.customer.id = :user_id AND od.car.id = :car_id")
    Optional<OrderDetail> findOrderDetailByCarIdAndUserId(@Param("user_id") Long user_id, @Param("car_id") Long car_id);

    // Tìm OrderDetail với đơn hàng đã hoàn tất cho 1 xe cụ thể của user (dùng cho bảo hành)
    @Query("SELECT od FROM OrderDetail od " +
       "JOIN od.order o " +
       "WHERE o.customer.id = :userId " + // Sửa ở đây: So sánh trực tiếp ID
       "AND od.car.id = :carId " +
       "AND o.status = 'COMPLETED'")
    Optional<OrderDetail> findCompletedOrderDetailByCarIdAndUserId(
        @Param("userId") Long userId, 
        @Param("carId") Long carId
    );

    // Lấy tất cả xe mà người dùng đã mua (không phụ thuộc trạng thái đơn hàng)
    @Query("SELECT DISTINCT od.car FROM OrderDetail od WHERE od.order.customer.id = :user_id")
    List<Car> findPurchasedCarsByUserId(@Param("user_id") Long user_id);
    // Lấy các đơn hàng mới được đặt
    @Query("""
            SELECT od FROM OrderDetail od
            WHERE od.car.seller.id = :sellerId
            AND od.order.status = :status
            """)
    List<OrderDetail> findBySellerAndStatus(
        @Param("sellerId") Long sellerId,
        @Param("status") Order.Status status
    );
}