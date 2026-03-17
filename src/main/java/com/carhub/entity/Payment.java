package com.carhub.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;
    private String status;
    private Double amount;
    private LocalDateTime paymentDate;
    public enum TypePayment {
        CAST, TRANSFER
    }

    @Enumerated(EnumType.STRING)
    private TypePayment typePayment;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;
    // Kết nối tới WarrentyTicket
    @OneToOne(mappedBy = "payment", cascade = CascadeType.ALL)
    private WarrantyTicket warrantyTicket;
}
