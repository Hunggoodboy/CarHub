package com.carhub.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    private String username;
    private String password; // Lưu ý: Nên mã hóa trước khi lưu
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;

    @Enumerated(EnumType.STRING)
    private Role role; // Enum: ADMIN, CUSTOMER, USER
    public enum Role {
        ADMIN, CUSTOMER
    }
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    List<Order> orders;

}
