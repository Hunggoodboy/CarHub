package com.carhub.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class CarImagesSub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;
    @Column(name = "image_url")
    private String imageUrl;

    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id")
    private Car car;
}