package com.carhub.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;
    private String name, model, color;
    @Column(columnDefinition = "Text", name = "image_url")
    private String imageUrl;
    @Column(columnDefinition = "TEXT")
    private String description;
    private double price, discount;
    private int manufactureYear, stockQuantity;
    //Kết nối qua BrandEntity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;
    //Kết nối tới OrderDetailEntity
    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails;

    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL)
    private List<Reviews> review;

    //Kết nối tới User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    //Kết nối tới CarImagesSubEntity
    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<CarImagesSub> subImages;
}
