package com.carhub.dto.Response;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderAdminDTO {
    private Long id;
    private String customerName;
    private String carName;
    private String status;
    private String paymentStatus;
    private LocalDateTime orderDate;
    private Double totalPrice;
    private String shippingAddress;
    private String phone;
    private Long quantity;
    private String paymentMethod;
}
