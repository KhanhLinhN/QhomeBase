package com.QhomeBase.servicescardservice.dto;

import java.util.UUID;

public record ElevatorCardRegistrationCreateDto(
        String fullName,
        String apartmentNumber,
        String buildingName,
        String requestType,
        String citizenId,
        String phoneNumber,
        String note,
        UUID unitId,
        UUID residentId
) {}

