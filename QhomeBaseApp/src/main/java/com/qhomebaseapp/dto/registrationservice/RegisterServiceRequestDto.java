package com.qhomebaseapp.dto.registrationservice;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterServiceRequestDto {

    private String serviceType;
    private String note;
    private String vehicleType;
    private String licensePlate;
    private String vehicleBrand;
    private String vehicleColor;

    private List<String> imageUrls;

    private String description;
    private String phoneNumber;
    private String address;
}
