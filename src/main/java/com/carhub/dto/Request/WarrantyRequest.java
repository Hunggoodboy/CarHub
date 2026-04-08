package com.carhub.dto.Request;


import lombok.Data;

@Data
public class WarrantyRequest {
    private Long carId;           
    private String street;        
    private String ward;          
    private String city;          
    private String phone;        
    private String defectDescription; 
}
