package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.BuildingDeletionApproveReq;
import com.QhomeBase.baseservice.dto.BuildingDeletionCreateReq;
import com.QhomeBase.baseservice.dto.BuildingDeletionRejectReq;
import com.QhomeBase.baseservice.dto.BuildingDeletionRequestDto;
import com.QhomeBase.baseservice.model.BuildingDeletionRequest;
import com.QhomeBase.baseservice.model.BuildingDeletionStatus;
import com.QhomeBase.baseservice.model.BuildingStatus;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.model.UnitStatus;
import com.QhomeBase.baseservice.repository.BuildingDeletionRequestRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.repository.buildingRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuildingDeletionService {
    private final BuildingDeletionRequestRepository repo;
    private final buildingRepository buildingRepository;
    private final UnitRepository unitRepository;

    private static BuildingDeletionRequestDto toDto(BuildingDeletionRequest t) {
        return new BuildingDeletionRequestDto(
                t.getId(),
                t.getTenantId(),
                t.getBuildingId(),
                t.getRequestedBy(),
                t.getReason(),
                t.getApprovedBy(),
                t.getNote(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getApprovedAt()
        );
    }

    public BuildingDeletionRequestDto create(BuildingDeletionCreateReq dto, Authentication auth) {
        var p = (UserPrincipal) auth.getPrincipal();

        var building = buildingRepository.findById(dto.buildingId())
                .orElseThrow(() -> new IllegalArgumentException("Building not found"));
        
        if (!building.getTenantId().equals(dto.tenantId())) {
            throw new IllegalArgumentException("Building does not belong to the specified tenant");
        }
        var existingRequest = repo.findByTenantIdAndBuildingId(dto.tenantId(), dto.buildingId())
                .stream()
                .filter(req -> req.getStatus() == BuildingDeletionStatus.PENDING)
                .findFirst();
        
        if (existingRequest.isPresent()) {
            throw new IllegalStateException("There is already a pending deletion request for this building");
        }
        building.setStatus(BuildingStatus.PENDING_DELETION);
        buildingRepository.save(building);
        var e = BuildingDeletionRequest.builder()
                .tenantId(dto.tenantId())
                .buildingId(dto.buildingId())
                .reason(dto.reason())
                .requestedBy(p.uid())
                .status(BuildingDeletionStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .build();

        repo.save(e);
        return toDto(e);
    }
    public BuildingDeletionRequestDto approve(UUID requestId, BuildingDeletionApproveReq req, Authentication auth) {
        var p = (UserPrincipal) auth.getPrincipal();
        var e = repo.findById(requestId).orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (e.getStatus() != BuildingDeletionStatus.PENDING) {
            throw new IllegalStateException("Request is not PENDING");
        }

        var building = buildingRepository.findById(e.getBuildingId()).orElseThrow(() -> new IllegalArgumentException("Building not found"));
        
        if (building.getStatus() == BuildingStatus.PENDING_DELETION) {
            building.setStatus(BuildingStatus.DELETING);
            buildingRepository.save(building);
        }

        e.setApprovedBy(p.uid());
        e.setNote(req.note());
        e.setApprovedAt(OffsetDateTime.now());
        e.setStatus(BuildingDeletionStatus.APPROVED);
        repo.save(e);
        changeStatusOfUnitsBuilding(e.getBuildingId());
        return toDto(e);
    }

    public BuildingDeletionRequestDto reject(UUID requestId, BuildingDeletionRejectReq req, Authentication auth) {
        var p = (UserPrincipal) auth.getPrincipal();
        var e = repo.findById(requestId).orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (e.getStatus() != BuildingDeletionStatus.PENDING) {
            throw new IllegalStateException("Request is not PENDING");
        }
        var building = buildingRepository.findById(e.getBuildingId()).orElseThrow(() -> new IllegalArgumentException("Building not found"));

        if (building.getStatus() == BuildingStatus.PENDING_DELETION) {
            building.setStatus(BuildingStatus.ACTIVE);
            buildingRepository.save(building);
        }

        e.setApprovedBy(p.uid());
        e.setNote(req.note());
        e.setApprovedAt(OffsetDateTime.now());
        e.setStatus(BuildingDeletionStatus.REJECTED);
        repo.save(e);
        
        return toDto(e);
    }

    public BuildingDeletionRequestDto getById(UUID requestId) {
        var e = repo.findById(requestId).orElseThrow(() -> new IllegalArgumentException("Request not found"));
        return toDto(e);
    }

    public List<BuildingDeletionRequestDto> getByTenantId(UUID tenantId) {
        return repo.findByTenantIdAndBuildingId(tenantId, null)
                .stream()
                .map(BuildingDeletionService::toDto)
                .toList();
    }

    public List<BuildingDeletionRequestDto> getByBuildingId(UUID buildingId) {
        return repo.findByTenantIdAndBuildingId(null, buildingId)
                .stream()
                .map(BuildingDeletionService::toDto)
                .toList();
    }

    public List<BuildingDeletionRequestDto> getPendingRequests() {
        return repo.findAll()
                .stream()
                .filter(req -> req.getStatus() == BuildingDeletionStatus.PENDING)
                .map(BuildingDeletionService::toDto)
                .toList();
    }
    public void changeStatusOfUnitsBuilding(UUID buildingId) {
        var b = unitRepository.findAllByBuildingId(buildingId);
        for (Unit u : b) {
            if (u.getStatus().equals(UnitStatus.ACTIVE)) {
                u.setStatus(UnitStatus.INACTIVE);
            }
        }
        unitRepository.saveAll(b);
    }


}
