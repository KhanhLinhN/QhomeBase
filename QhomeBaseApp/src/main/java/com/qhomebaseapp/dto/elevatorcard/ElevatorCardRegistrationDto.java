package com.qhomebaseapp.dto.elevatorcard;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElevatorCardRegistrationDto {

    private String fullName;
    private String apartmentNumber;
    private String buildingName;
    private String requestType; // NEW_CARD, REPLACE_CARD
    private String citizenId;
    private String phoneNumber;
    private String note;
}

