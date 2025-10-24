package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.client.FinanceBillingClient;
import com.QhomeBase.baseservice.dto.VehicleActivatedEvent;
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
    private final FinanceBillingClient financeBillingClient;

    private OffsetDateTime nowUTC() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    public VehicleRegistrationDto createRegistrationRequest(VehicleRegistrationCreateDto dto, Authentication authentication) {
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
        var u = (UserPrincipal) authentication.getPrincipal();
        UUID approvedBy = u.uid();
        var request = vehicleRegistrationRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Registration request not found"));

        if (request.getStatus() != VehicleRegistrationStatus.PENDING) {
            throw new IllegalStateException("Request is not PENDING");
        }

        request.setApprovedBy(approvedBy);
        request.setNote(dto.note());
        request.setApprovedAt(nowUTC());
        request.setStatus(VehicleRegistrationStatus.APPROVED);
        request.setUpdatedAt(nowUTC());

        var savedRequest = vehicleRegistrationRepository.save(request);
        
        notifyVehicleActivated(savedRequest);

        return toDto(savedRequest);
    }

    public VehicleRegistrationDto rejectRequest(UUID requestId, VehicleRegistrationRejectDto dto, Authentication authentication) {
        var u = (UserPrincipal) authentication.getPrincipal();
        UUID rejectedBy = u.uid();
        var request = vehicleRegistrationRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Registration request not found"));

        if (request.getStatus() != VehicleRegistrationStatus.PENDING) {
            throw new IllegalStateException("Request is not PENDING");
        }

        request.setApprovedBy(rejectedBy);
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
                .orElseThrow(() -> new IllegalArgumentException("Registration request not found"));

        if (request.getStatus() != VehicleRegistrationStatus.PENDING) {
            throw new IllegalStateException("Request is not PENDING");
        }

        boolean isRequester = request.getRequestedBy().equals(userId);
        boolean hasManagerRole = u.roles() != null && 
            (u.roles().contains("tenant_manager") || 
             u.roles().contains("tenant_owner") || 
             u.roles().contains("admin"));
        
        if (!isRequester && !hasManagerRole) {
            throw new IllegalStateException("Only the requester or tenant manager can cancel the request");
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

    private void notifyVehicleActivated(VehicleRegistrationRequest request) {
        OffsetDateTime now = nowUTC();
        int dayOfMonth = now.getDayOfMonth();

        if (dayOfMonth <= 5) {
            return;
        }

        var vehicle = request.getVehicle();
        if (vehicle == null) {
            return;
        }

        var event = VehicleActivatedEvent.builder()
                .vehicleId(vehicle.getId())
                .tenantId(request.getTenantId())
                .unitId(vehicle.getUnit() != null ? vehicle.getUnit().getId() : null)
                .residentId(vehicle.getResident() != null ? vehicle.getResident().getId() : null)
                .plateNo(vehicle.getPlateNo())
                .vehicleKind(vehicle.getKind() != null ? vehicle.getKind().name() : null)
                .activatedAt(now)
                .approvedBy(request.getApprovedBy())
                .build();

        financeBillingClient.notifyVehicleActivatedSync(event);
    }

    public VehicleRegistrationDto toDto(VehicleRegistrationRequest request) {
        UUID vehicleId;
        String vehiclePlateNo;
        String vehicleKind;
        String vehicleColor;

        try {
            if (request.getVehicle() != null) {
                vehicleId = request.getVehicle().getId();
                vehiclePlateNo = request.getVehicle().getPlateNo();
                vehicleKind = request.getVehicle().getKind() != null ? request.getVehicle().getKind().name() : null;
                vehicleColor = request.getVehicle().getColor();
            } else {
                vehicleId = null;
                vehiclePlateNo = null;
                vehicleKind = null;
                vehicleColor = null;
            }
        } catch (Exception e) {
            vehicleId = null;
            vehiclePlateNo = "Unknown";
            vehicleKind = "Unknown";
            vehicleColor = "Unknown";
        }

        String requestedByName = "Unknown";
        String approvedByName = request.getApprovedBy() != null ? "Unknown" : null;

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
}
