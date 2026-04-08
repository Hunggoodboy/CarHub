package com.carhub.dto.Response;

import lombok.Data;
import java.util.Date;

@Data
public class WarrantyResponseDTO {
    private Long id;
    private String carModel;
    private String customerName;
    private String phone;
    private String street;
    private String ward;
    private String city;
    private String defectDescription;
    private String status;
    private Date receivedDate;
}