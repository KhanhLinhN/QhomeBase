package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.TenantDeletionRequestDTO;
import com.QhomeBase.baseservice.model.*;
import com.QhomeBase.baseservice.repository.TenantDeletionRequestRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.repository.BuildingRepository;
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
    private final BuildingRepository buildingRepository;
    private final TenantDeletionRequestRepository repo;
    private final UnitRepository unitRepository;
    public void changeStatusOfBuilding(UUID tenantid) {
        List<Building> Buildings = buildingRepository.findAllByTenantIdOrderByCodeAsc(tenantid);
        for (Building building : Buildings) {
            if (building.getStatus().equals(BuildingStatus.ACTIVE)) {
                building.setStatus(BuildingStatus.PENDING_DELETION);
            }
        }
    }

    public void changeStatusOfUnits(UUID tenantId) {
        List<Unit> units = unitRepository.findAllByTenantId(tenantId);
        for (Unit unit : units) {
            if (unit.getStatus().equals(UnitStatus.ACTIVE)) {
                unit.setStatus(UnitStatus.INACTIVE);
            }
        }
        unitRepository.saveAll(units);
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
        if (reason == null) {
            throw new IllegalArgumentException("reason is null");
        }
        if ( repo.countPendingRequestsByTenantId(tenantId)>0) {
            throw new IllegalArgumentException("A PENDING had 2");
        }
        if (repo.findDeletedTenant(tenantId)>0) {
            throw new IllegalArgumentException("A tenant deleted");
        }
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


        changeStatusOfBuilding(e.getTenantId());
        changeStatusOfUnits(e.getTenantId());

        e.setApprovedBy(p.uid());
        e.setNote(note);
        e.setApprovedAt(OffsetDateTime.now());
        e.setStatus(TenantDeletionStatus.APPROVED);
        repo.save(e);
        return toDTO(e);
    }


}
