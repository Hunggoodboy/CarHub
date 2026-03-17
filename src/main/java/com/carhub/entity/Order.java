package com.carhub.entity;

import com.carhub.dto.OrderRequest;
import jakarta.persistence.*;
import lombok.Data;
import org.aspectj.weaver.ast.Or;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;
    private Long buyerId;
    private Long sellerId;
    LocalDateTime orderDate;
    Double totalAmountOriginal,totalAmountFinal, totalDiscount;
    String deliveryAddress;
    private String street;
    private String ward;
    private String city;
    private String phone;
    public enum Status{
        PENDING,
        DELIVERED,
        CANCELLED,
        COMPLETED
    }
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.PENDING;
    //Kết nối OrderDetail
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails;

    //Kết nối Payment
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;


}
