package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.VehicleRegistrationApproveDto;
import com.QhomeBase.baseservice.dto.VehicleRegistrationCreateDto;
import com.QhomeBase.baseservice.dto.VehicleRegistrationDto;
import com.QhomeBase.baseservice.dto.VehicleRegistrationRejectDto;
import com.QhomeBase.baseservice.model.VehicleRegistrationRequest;
import com.QhomeBase.baseservice.model.VehicleRegistrationStatus;
import com.QhomeBase.baseservice.repository.VehicleRegistrationRepository;
import com.QhomeBase.baseservice.repository.VehicleRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleRegistrationService {
    private final VehicleRegistrationRepository vehicleRegistrationRepository;
    private final VehicleRepository vehicleRepository;

    private OffsetDateTime nowUTC() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    public VehicleRegistrationDto createRegistrationRequest(VehicleRegistrationCreateDto dto, Authentication authentication) {
        validateVehicleRegistrationCreateDto(dto);
        
        var u = (UserPrincipal) authentication.getPrincipal();
        UUID requestById = u.uid();
        if (vehicleRegistrationRepository.existsByTenantIdAndVehicleId(dto.tenantId(), dto.vehicleId())) {
            throw new IllegalStateException("Registration request for this vehicle already exists");
        }

        var request = VehicleRegistrationRequest.builder()
                .tenantId(dto.tenantId())
                .vehicle(vehicleRepository.findById(dto.vehicleId())
                        .orElseThrow())
                .reason(dto.reason())
                .status(VehicleRegistrationStatus.PENDING)
                .requestedBy(requestById)
                .requestedAt(nowUTC())
                .createdAt(nowUTC())
                .updatedAt(nowUTC())
                .build();

        var savedRequest = vehicleRegistrationRepository.save(request);
        return toDto(savedRequest);
    }

    public VehicleRegistrationDto approveRequest(UUID requestId, VehicleRegistrationApproveDto dto, Authentication authentication) {
        validateVehicleRegistrationApproveDto(dto);
        
        var u = (UserPrincipal) authentication.getPrincipal();
        UUID requestById = u.uid();
        var request = vehicleRegistrationRepository.findById(requestId)
                .orElseThrow();

        if (request.getStatus() != VehicleRegistrationStatus.PENDING) {
            throw new IllegalStateException("Request is not PENDING");
        }

        request.setApprovedBy(requestById);
        request.setNote(dto.note());
        request.setApprovedAt(nowUTC());
        request.setStatus(VehicleRegistrationStatus.APPROVED);
        request.setUpdatedAt(nowUTC());

        var savedRequest = vehicleRegistrationRepository.save(request);
        return toDto(savedRequest);
    }

    public VehicleRegistrationDto rejectRequest(UUID requestId, VehicleRegistrationRejectDto dto, Authentication authentication) {
        validateVehicleRegistrationRejectDto(dto);
        
        var u = (UserPrincipal) authentication.getPrincipal();
        UUID requestById = u.uid();
        var request = vehicleRegistrationRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Registration request not found"));

        if (request.getStatus() != VehicleRegistrationStatus.PENDING) {
            throw new IllegalStateException("Request is not PENDING");
        }

        request.setApprovedBy(requestById);
        request.setNote(dto.reason());
        request.setApprovedAt(nowUTC());
        request.setStatus(VehicleRegistrationStatus.REJECTED);
        request.setUpdatedAt(nowUTC());

        var savedRequest = vehicleRegistrationRepository.save(request);
        return toDto(savedRequest);
    }

    public VehicleRegistrationDto cancelRequest(UUID requestId, Authentication authentication) {
        var u = (UserPrincipal) authentication.getPrincipal();
        UUID userId = u.uid();
        
        var request = vehicleRegistrationRepository.findById(requestId)
                .orElseThrow();

        if (request.getStatus() != VehicleRegistrationStatus.PENDING) {
            throw new IllegalStateException("Request is not PENDING");
        }

        if (!request.getRequestedBy().equals(userId)) {
            throw new IllegalStateException("Only the requester can cancel the request");
        }

        request.setStatus(VehicleRegistrationStatus.CANCELED);
        request.setUpdatedAt(nowUTC());

        var savedRequest = vehicleRegistrationRepository.save(request);
        return toDto(savedRequest);
    }

    public VehicleRegistrationDto getRequestById(UUID id) {
        var request = vehicleRegistrationRepository.findByIdWithVehicle(id);
        if (request == null) {
            throw new IllegalArgumentException("Registration request not found");
        }
        return toDto(request);
    }

    public List<VehicleRegistrationDto> getRequestsByTenantId(UUID tenantId) {
        var requests = vehicleRegistrationRepository.findAllByTenantId(tenantId);
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    public List<VehicleRegistrationDto> getRequestsByStatus(VehicleRegistrationStatus status) {
        var requests = vehicleRegistrationRepository.findAllByStatus(status);
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    public List<VehicleRegistrationDto> getPendingRequests() {
        return getRequestsByStatus(VehicleRegistrationStatus.PENDING);
    }

    public List<VehicleRegistrationDto> getRequestsByVehicleId(UUID vehicleId) {
        var requests = vehicleRegistrationRepository.findAllByVehicleId(vehicleId);
        return requests.stream()
                .map(this::toDto)
                .toList();
    }

    public VehicleRegistrationDto toDto(VehicleRegistrationRequest request) {
        UUID vehicleId = null;
        String vehiclePlateNo = null;
        String vehicleKind = null;
        String vehicleColor = null;
        String requestedByName = null;
        String approvedByName = null;

        try {
            if (request.getVehicle() != null) {
                vehicleId = request.getVehicle().getId();
                vehiclePlateNo = request.getVehicle().getPlateNo();
                vehicleKind = request.getVehicle().getKind() != null ? request.getVehicle().getKind().name() : null;
                vehicleColor = request.getVehicle().getColor();
            }
        } catch (Exception e) {
            vehicleId = null;
            vehiclePlateNo = "Unknown";
            vehicleKind = "Unknown";
            vehicleColor = "Unknown";
        }

        requestedByName = "Unknown";
        approvedByName = request.getApprovedBy() != null ? "Unknown" : null;

        return new VehicleRegistrationDto(
                request.getId(),
                request.getTenantId(),
                vehicleId,
                vehiclePlateNo,
                vehicleKind,
                vehicleColor,
                request.getReason(),
                request.getStatus(),
                request.getRequestedBy(),
                requestedByName,
                request.getApprovedBy(),
                approvedByName,
                request.getNote(),
                request.getRequestedAt(),
                request.getApprovedAt(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    private void validateVehicleRegistrationCreateDto(VehicleRegistrationCreateDto dto) {
        if (dto.tenantId() == null) {
            throw new NullPointerException("Tenant ID cannot be null");
        }
        if (dto.vehicleId() == null) {
            throw new NullPointerException("Vehicle ID cannot be null");
        }
        if (dto.reason() != null && dto.reason().length() > 500) {
            throw new IllegalArgumentException("Reason cannot exceed 500 characters");
        }
    }

    private void validateVehicleRegistrationApproveDto(VehicleRegistrationApproveDto dto) {
        if (dto.approvedBy() == null) {
            throw new NullPointerException("Approved by cannot be null");
        }
        if (dto.note() != null && dto.note().length() > 500) {
            throw new IllegalArgumentException("Note cannot exceed 500 characters");
        }
    }

    private void validateVehicleRegistrationRejectDto(VehicleRegistrationRejectDto dto) {
        if (dto.rejectedBy() == null) {
            throw new NullPointerException("Rejected by cannot be null");
        }
        if (dto.reason() == null) {
            throw new NullPointerException("Reason cannot be null");
        }
        if (dto.reason().trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be empty");
        }
        if (dto.reason().length() > 500) {
            throw new IllegalArgumentException("Reason cannot exceed 500 characters");
        }
    }
}
