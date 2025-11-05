package com.qhomebaseapp.dto.residentcard;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResidentCardRegistrationDto {

    private String residentName;
    private String apartmentNumber;
    private String buildingName;
    private String citizenId;
    private String phoneNumber;
    private String note;
}

