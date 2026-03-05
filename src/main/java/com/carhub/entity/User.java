package com.carhub.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;
    @Column(nullable = true)
    private String username;
    @Column(nullable = true)
    private String password; // Lưu ý: Nên mã hóa trước khi lưu
    @Column(nullable = true)
    private String fullName;
    private String email;
    @Column(nullable = true)
    private String phoneNumber;
    @Column(nullable = true)
    private String address;

    @Enumerated(EnumType.STRING)
    private Role role; // Enum: ADMIN, CUSTOMER, USER
    public enum Role {
        ADMIN, CUSTOMER
    }


}
