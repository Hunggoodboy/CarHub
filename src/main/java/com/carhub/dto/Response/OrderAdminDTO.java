package com.carhub.dto.Response;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderAdminDTO {
    private Long id;
    private String username;
    private String carName;
    private String status;
    private String paymentStatus;

}
