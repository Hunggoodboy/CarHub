package com.carhub.entity;


import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;
    private String name, origin;

    @OneToMany(mappedBy = "brand", cascade = CascadeType.ALL)
    private List<Car> cars = new ArrayList<>();

}
