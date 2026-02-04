package com.fnb.carhub.entity;

import com.fnb.carhub.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Entity
@Data
@DiscriminatorValue("ADMIN") // Giá trị để phân biệt đây là Admin
public class Admin extends User {

    private String adminId; // Trường riêng của Admin trong UML

    // --- Quan hệ với ConsultationRequest (Admin trả lời) ---
    @OneToMany(mappedBy = "admin")
    private List<ConsultationRequest> repliedConsultations;

    // --- Quan hệ với WarrantyTicket (Admin tạo/xử lý) ---
    @OneToMany(mappedBy = "admin")
    private List<WarrantyTicket> managedWarranties;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}