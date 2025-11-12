package com.QhomeBase.servicescardservice.service;

import com.QhomeBase.servicescardservice.dto.CardRegistrationSummaryDto;
import com.QhomeBase.servicescardservice.model.ElevatorCardRegistration;
import com.QhomeBase.servicescardservice.model.RegisterServiceRequest;
import com.QhomeBase.servicescardservice.model.ResidentCardRegistration;
import com.QhomeBase.servicescardservice.repository.ElevatorCardRegistrationRepository;
import com.QhomeBase.servicescardservice.repository.RegisterServiceRequestRepository;
import com.QhomeBase.servicescardservice.repository.ResidentCardRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardRegistrationQueryService {

    private static final String CARD_TYPE_RESIDENT = "RESIDENT_CARD";
    private static final String CARD_TYPE_ELEVATOR = "ELEVATOR_CARD";
    private static final String CARD_TYPE_VEHICLE = "VEHICLE_CARD";
    private static final String VEHICLE_SERVICE_TYPE = "VEHICLE_REGISTRATION";

    private final ResidentCardRegistrationRepository residentCardRepository;
    private final ElevatorCardRegistrationRepository elevatorCardRepository;
    private final RegisterServiceRequestRepository vehicleRegistrationRepository;

    @Transactional(readOnly = true)
    public List<CardRegistrationSummaryDto> getCardRegistrations(UUID userId, UUID residentId, UUID unitId) {
        List<CardRegistrationSummaryDto> items = new ArrayList<>();

        if (residentId != null) {
            List<ResidentCardRegistration> residentCards = fetchResidentCards(userId, residentId, unitId);
            residentCards.stream()
                    .map(this::mapResidentCard)
                    .forEach(items::add);

            List<ElevatorCardRegistration> elevatorCards = fetchElevatorCards(userId, residentId, unitId);
            elevatorCards.stream()
                    .map(this::mapElevatorCard)
                    .forEach(items::add);
        }

        if (userId != null) {
            List<RegisterServiceRequest> vehicleCards = fetchVehicleCards(userId, unitId);
            vehicleCards.stream()
                    .filter(request -> VEHICLE_SERVICE_TYPE.equalsIgnoreCase(request.getServiceType()))
                    .map(this::mapVehicleCard)
                    .forEach(items::add);
        }

        items.sort(Comparator.comparing(CardRegistrationSummaryDto::createdAt,
                Comparator.nullsLast(OffsetDateTime::compareTo)).reversed());
        return items;
    }

    private List<ResidentCardRegistration> fetchResidentCards(UUID userId, UUID residentId, UUID unitId) {
        if (unitId != null) {
            return residentCardRepository.findByResidentIdAndUnitId(residentId, unitId);
        }
        List<ResidentCardRegistration> cards = residentCardRepository.findByResidentId(residentId);
        if (CollectionUtils.isEmpty(cards)) {
            if (userId != null) {
                return residentCardRepository.findByUserId(userId);
            }
        }
        return cards;
    }

    private List<ElevatorCardRegistration> fetchElevatorCards(UUID userId, UUID residentId, UUID unitId) {
        if (unitId != null) {
            return elevatorCardRepository.findByResidentIdAndUnitId(residentId, unitId);
        }
        List<ElevatorCardRegistration> cards = elevatorCardRepository.findByResidentId(residentId);
        if (CollectionUtils.isEmpty(cards)) {
            if (userId != null) {
                return elevatorCardRepository.findByUserId(userId);
            }
        }
        return cards;
    }

    private List<RegisterServiceRequest> fetchVehicleCards(UUID userId, UUID unitId) {
        if (unitId != null) {
            return vehicleRegistrationRepository.findByUserIdAndUnitId(userId, unitId);
        }
        return vehicleRegistrationRepository.findByUserId(userId);
    }

    private CardRegistrationSummaryDto mapResidentCard(ResidentCardRegistration entity) {
        return new CardRegistrationSummaryDto(
                entity.getId(),
                CARD_TYPE_RESIDENT,
                entity.getUserId(),
                entity.getResidentId(),
                entity.getUnitId(),
                normalize(entity.getRequestType()),
                normalize(entity.getStatus()),
                normalize(entity.getPaymentStatus()),
                entity.getPaymentAmount(),
                entity.getPaymentDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                firstNonBlank(entity.getFullName(), "Đăng ký thẻ cư dân"),
                firstNonBlank(entity.getApartmentNumber(), entity.getCitizenId()),
                entity.getApartmentNumber(),
                entity.getBuildingName(),
                entity.getNote()
        );
    }

    private CardRegistrationSummaryDto mapElevatorCard(ElevatorCardRegistration entity) {
        return new CardRegistrationSummaryDto(
                entity.getId(),
                CARD_TYPE_ELEVATOR,
                entity.getUserId(),
                entity.getResidentId(),
                entity.getUnitId(),
                normalize(entity.getRequestType()),
                normalize(entity.getStatus()),
                normalize(entity.getPaymentStatus()),
                entity.getPaymentAmount(),
                entity.getPaymentDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                firstNonBlank(entity.getFullName(), "Đăng ký thẻ thang máy"),
                firstNonBlank(entity.getApartmentNumber(), entity.getCitizenId()),
                entity.getApartmentNumber(),
                entity.getBuildingName(),
                entity.getNote()
        );
    }

    private CardRegistrationSummaryDto mapVehicleCard(RegisterServiceRequest entity) {
        BigDecimal amount = entity.getPaymentAmount();
        return new CardRegistrationSummaryDto(
                entity.getId(),
                CARD_TYPE_VEHICLE,
                entity.getUserId(),
                null,
                entity.getUnitId(),
                normalize(entity.getRequestType()),
                normalize(entity.getStatus()),
                normalize(entity.getPaymentStatus()),
                amount,
                entity.getPaymentDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                firstNonBlank(entity.getLicensePlate(), "Đăng ký thẻ xe"),
                firstNonBlank(entity.getVehicleType(), entity.getBuildingName()),
                entity.getApartmentNumber(),
                entity.getBuildingName(),
                entity.getNote()
        );
    }

    private String normalize(String value) {
        return value != null ? value.trim().toUpperCase(Locale.ROOT) : null;
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback;
    }
}


