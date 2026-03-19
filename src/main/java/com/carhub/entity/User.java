package com.carhub.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Data
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "users")
public class User implements UserDetails {
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

    public enum Role {
        ADMIN, CUSTOMER
    }

    @Enumerated(EnumType.STRING)
    private Role role = Role.CUSTOMER; // Enum: ADMIN, CUSTOMER, USER

    public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL)
    private List<Car> cars;

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL)
    private List<ChatMessage> sentMessages;

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL)
    private List<ChatMessage> receivedMessages;
}
