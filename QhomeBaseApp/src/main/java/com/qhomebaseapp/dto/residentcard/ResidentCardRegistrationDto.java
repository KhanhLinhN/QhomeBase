package com.qhomebaseapp.dto.residentcard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResidentCardRegistrationDto {

    private String residentName;
    private String apartmentNumber;
    private String buildingName;
    private String requestType; // NEW_CARD, REPLACE_CARD
    private String citizenId;
    private String phoneNumber;
    private String note;
}

