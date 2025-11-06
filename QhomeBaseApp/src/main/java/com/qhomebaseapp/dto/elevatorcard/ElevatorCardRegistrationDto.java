package com.qhomebaseapp.dto.elevatorcard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElevatorCardRegistrationDto {

    private String fullName;
    private String apartmentNumber;
    private String buildingName;
    private String requestType; // NEW_CARD, REPLACE_CARD
    private String citizenId;
    private String phoneNumber;
    private String note;
}

