package com.carhub.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Customer extends User{
    private String shippingAddress;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Order> orders;

    // --- Quan hệ với ConsultationRequest (Khách hàng gửi câu hỏi) ---
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<ConsultationRequest> consultationRequests;

    // --- Quan hệ với WarrantyTicket (Khách hàng sở hữu phiếu bảo hành) ---
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<WarrantyTicket> warrantyTickets;
}
