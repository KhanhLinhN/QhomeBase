package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.TenantDeletionRequestDTO;
import com.QhomeBase.baseservice.model.BuildingStatus;
import com.QhomeBase.baseservice.model.TenantDeletionRequest;
import com.QhomeBase.baseservice.model.TenantDeletionStatus;
import com.QhomeBase.baseservice.model.building;
import com.QhomeBase.baseservice.repository.TenantDeletionRequestRepository;
import com.QhomeBase.baseservice.repository.buildingRepository;
import com.QhomeBase.baseservice.repository.tenantRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantDeletionService {
    private final buildingRepository buildingRepository;
    private final tenantRepository tenantRepository;
    private final TenantDeletionRequestRepository repo;
    public void changeStatusOfBuilding(UUID tenantid) {
        List<building> buildings = buildingRepository.findAllByTenantIdOrderByCodeAsc(tenantid);
        for (building building : buildings) {
            if (building.getStatus().equals(BuildingStatus.ACTIVE)) {
                building.setStatus(BuildingStatus.PENDING_DELETION);
            }
        }
    }
    public TenantDeletionRequestDTO getTenantDeletionRequestDTO(UUID ticketTenantId) {
        var e = repo.findById(ticketTenantId).orElseThrow();
        return toDTO(e);
    }
    private static TenantDeletionRequestDTO toDTO(TenantDeletionRequest e) {
        return new TenantDeletionRequestDTO(
                e.getId(), e.getTenantId(), e.getRequestedBy(), e.getApprovedBy(),
                e.getReason(), e.getNote(), e.getStatus(), e.getCreatedAt(), e.getApprovedAt()
        );
    }

    public TenantDeletionRequestDTO create(UUID tenantId, String reason, Authentication auth) {
        var p = (UserPrincipal) auth.getPrincipal();
        var e = TenantDeletionRequest.builder()
                .tenantId(tenantId)
                .requestedBy(p.uid())
                .reason(reason)
                .status(TenantDeletionStatus.PENDING)
                .build();
        repo.save(e);
        return toDTO(e);
    }
    public TenantDeletionRequestDTO approve(UUID ticketId, String note, Authentication auth) {
        var p = (UserPrincipal) auth.getPrincipal();
        var e = repo.findById(ticketId).orElseThrow();

        if (e.getStatus() != TenantDeletionStatus.PENDING) {
            throw new IllegalStateException("Ticket is not PENDING");
        }

        e.setApprovedBy(p.uid());
        e.setNote(note);
        e.setApprovedAt(OffsetDateTime.now());
        e.setStatus(TenantDeletionStatus.APPROVED);
        return toDTO(e);
    }


}
