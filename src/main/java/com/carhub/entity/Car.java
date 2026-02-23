package com.carhub.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.*;

import java.util.List;

@Entity
@Data
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;
    private String name, model, color, description, imageUrl;
    private double price, discount;
    private int manufactureYear, stockQuantity;
    //Kết nối qua BrandEntity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;
    //Kết nối tới OrderDetailEntity
    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails;

}
