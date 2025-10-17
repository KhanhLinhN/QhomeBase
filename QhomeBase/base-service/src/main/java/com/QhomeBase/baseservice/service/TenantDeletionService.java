package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.TenantDeletionRequestDTO;
import com.QhomeBase.baseservice.model.*;
import com.QhomeBase.baseservice.repository.TenantDeletionRequestRepository;
import com.QhomeBase.baseservice.repository.TenantRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import com.QhomeBase.baseservice.repository.BuildingDeletionRequestRepository;
import com.QhomeBase.baseservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantDeletionService {
    private final BuildingRepository buildingRepository;
    private final TenantDeletionRequestRepository repo;
    private final UnitRepository unitRepository;
    private final TenantRepository tenantRepository;
    private final BuildingDeletionRequestRepository buildingDeletionRequestRepository;
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
        boolean changed = false;
        for (Unit unit : units) {
            if (unit.getStatus() == UnitStatus.ACTIVE) {
                unit.setStatus(UnitStatus.INACTIVE);
                changed = true;
            }
        }
        if (changed) unitRepository.saveAll(units);
    }

    private void updateBuildingsStatus(UUID tenantId, BuildingStatus from, BuildingStatus to) {
        System.out.println("=== updateBuildingsStatus Debug ===");
        System.out.println("Tenant ID: " + tenantId);
        System.out.println("From Status: " + from);
        System.out.println("To Status: " + to);
        
        // Check if tenant exists
        var tenant = tenantRepository.findById(tenantId);
        if (tenant.isEmpty()) {
            System.err.println("ERROR: Tenant not found with ID: " + tenantId);
            throw new IllegalArgumentException("Tenant not found with ID: " + tenantId);
        }
        System.out.println("Tenant found: " + tenant.get().getName());
        
        List<Building> buildings = buildingRepository.findAllByTenantIdOrderByCodeAsc(tenantId);
        System.out.println("Total buildings found: " + buildings.size());
        
        boolean changed = false;
        int updatedCount = 0;
        
        for (Building b : buildings) {
            System.out.println("Building ID: " + b.getId() + ", Current Status: " + b.getStatus());
            if (b.getStatus() == from) {
                b.setStatus(to);
                changed = true;
                updatedCount++;
                System.out.println("Updated building " + b.getId() + " from " + from + " to " + to);
            }
        }
        
        if (changed) {
            try {
                buildingRepository.saveAll(buildings);
                System.out.println("Successfully saved " + updatedCount + " buildings");
            } catch (Exception e) {
                System.err.println("Error saving buildings: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        } else {
            System.out.println("No buildings needed status update");
        }
        
        System.out.println("=== End updateBuildingsStatus Debug ===");
    }



    private boolean areAllTargetsReached(UUID tenantId) {

        List<Building> buildings = buildingRepository.findAllByTenantIdOrderByCodeAsc(tenantId);
        for (Building b : buildings) {
            if (b.getStatus() != BuildingStatus.ARCHIVED) {
                return false;
            }
        }
        List<Unit> units = unitRepository.findAllByTenantId(tenantId);
        for (Unit u : units) {
            if (u.getStatus() != UnitStatus.INACTIVE) {
                return false;
            }
        }
        return true;
    }

    public void completeTenantDeletionIfReady(UUID tenantId) {
        if (!areAllTargetsReached(tenantId)) {
            return;
        }
        tenantRepository.findById(tenantId).ifPresent(tenant -> {
            tenant.setStatus("ARCHIVED");
            tenant.setDeleted(true);
            tenantRepository.save(tenant);
        });
    }

    private void createBuildingDeletionRequests(UUID tenantId, UUID approvedBy) {
        System.out.println("=== createBuildingDeletionRequests Debug ===");
        System.out.println("Tenant ID: " + tenantId);
        System.out.println("Approved By: " + approvedBy);
        
        List<Building> buildings = buildingRepository.findAllByTenantIdOrderByCodeAsc(tenantId);
        System.out.println("Total buildings found for tenant: " + buildings.size());
        
        int deletingBuildingsCount = 0;
        int createdRequestsCount = 0;
        
        for (Building building : buildings) {
            System.out.println("Building ID: " + building.getId() + ", Status: " + building.getStatus());
            
            if (building.getStatus() == BuildingStatus.DELETING) {
                deletingBuildingsCount++;
                System.out.println("Creating BuildingDeletionRequest for building: " + building.getId());
                
                BuildingDeletionRequest request = BuildingDeletionRequest.builder()
                        .tenantId(tenantId)
                        .buildingId(building.getId())
                        .requestedBy(approvedBy)
                        .reason("Building deletion as part of tenant deletion process")
                        .status(BuildingDeletionStatus.APPROVED)
                        .approvedBy(approvedBy)
                        .note("Auto-approved during tenant deletion approval")
                        .createdAt(OffsetDateTime.now())
                        .approvedAt(OffsetDateTime.now())
                        .build();
                
                try {
                    BuildingDeletionRequest savedRequest = buildingDeletionRequestRepository.save(request);
                    createdRequestsCount++;
                    System.out.println("Successfully created BuildingDeletionRequest with ID: " + savedRequest.getId());
                } catch (Exception e) {
                    System.err.println("Error creating BuildingDeletionRequest for building " + building.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        System.out.println("Summary:");
        System.out.println("- Total buildings: " + buildings.size());
        System.out.println("- Buildings with DELETING status: " + deletingBuildingsCount);
        System.out.println("- BuildingDeletionRequests created: " + createdRequestsCount);
        System.out.println("=== End createBuildingDeletionRequests Debug ===");
    }
    public TenantDeletionRequestDTO getTenantDeletionRequestDTO(UUID ticketTenantId) {
        var e = repo.findById(ticketTenantId).orElseThrow();
        return toDTO(e);
    }

    public List<TenantDeletionRequestDTO> getAllTenantDeletionRequests() {
        List<TenantDeletionRequest> requests = repo.findAll();
        return requests.stream()
                .map(TenantDeletionService::toDTO)
                .toList();
    }

    public List<TenantDeletionRequestDTO> getTenantDeletionRequestsByTenantId(UUID tenantId) {
        return repo.findByTenantId(tenantId)
                .stream()
                .map(TenantDeletionService::toDTO)
                .toList();
    }

    public TenantDeletionRequestDTO completeTenantDeletion(UUID requestId, Authentication auth) {
        var request = repo.findById(requestId).orElseThrow();
        
        if (request.getStatus() != TenantDeletionStatus.APPROVED) {
            throw new IllegalStateException("Request must be APPROVED before completion");
        }
        

        Map<String, Object> targetsStatus = getTenantDeletionTargetsStatus(request.getTenantId());
        boolean allTargetsReady = (Boolean) targetsStatus.get("allTargetsReady");
        
        if (!allTargetsReady) {
            throw new IllegalStateException("All targets must be ARCHIVED/INACTIVE before completion. Current status: " + targetsStatus);
        }
        

        completeTenantDeletionIfReady(request.getTenantId());
        

        request.setStatus(TenantDeletionStatus.COMPLETED);
        repo.save(request);
        
        return toDTO(request);
    }

    public TenantDeletionRequestDTO rejectTenantDeletion(UUID requestId, String note, Authentication auth) {
        var p = (UserPrincipal) auth.getPrincipal();
        var request = repo.findById(requestId).orElseThrow();
        
        if (request.getStatus() != TenantDeletionStatus.PENDING) {
            throw new IllegalStateException("Request must be PENDING before rejection");
        }

        request.setStatus(TenantDeletionStatus.REJECTED);
        request.setNote(note);
        request.setApprovedBy(p.uid());
        request.setApprovedAt(OffsetDateTime.now());
        repo.save(request);
        
        return toDTO(request);
    }

    public Map<String, Object> getTenantDeletionTargetsStatus(UUID tenantId) {
        List<Building> buildings = buildingRepository.findAllByTenantIdOrderByCodeAsc(tenantId);
        List<Unit> units = unitRepository.findAllByTenantId(tenantId);
        
        Map<String, Object> status = new HashMap<>();
        

        Map<String, Long> buildingStatusCount = buildings.stream()
                .collect(Collectors.groupingBy(
                    b -> b.getStatus().toString(),
                    Collectors.counting()
                ));
        

        Map<String, Long> unitStatusCount = units.stream()
                .collect(Collectors.groupingBy(
                    u -> u.getStatus().toString(),
                    Collectors.counting()
                ));
        

        long buildingsArchived = buildingStatusCount.getOrDefault("ARCHIVED", 0L);
        long unitsInactive = unitStatusCount.getOrDefault("INACTIVE", 0L);
        

        boolean buildingsReady = buildings.size() == 0 || buildingsArchived == buildings.size();
        boolean unitsReady = units.size() == 0 || unitsInactive == units.size();
        boolean allTargetsReady = buildingsReady && unitsReady;
        
        status.put("buildings", buildingStatusCount);
        status.put("units", unitStatusCount);
        status.put("totalBuildings", buildings.size());
        status.put("totalUnits", units.size());
        status.put("buildingsArchived", buildingsArchived);
        status.put("unitsInactive", unitsInactive);
        status.put("buildingsReady", buildingsReady);
        status.put("unitsReady", unitsReady);
        status.put("allTargetsReady", allTargetsReady);
        
        status.put("requirements", Map.of(
            "buildings", "All buildings must be ARCHIVED",
            "units", "All units must be INACTIVE"
        ));
        
        return status;
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
        updateBuildingsStatus(tenantId, BuildingStatus.ACTIVE, BuildingStatus.PENDING_DELETION);
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

        System.out.println("=== approve method Debug ===");
        System.out.println("Ticket ID: " + ticketId);
        System.out.println("Request Status: " + e.getStatus());
        System.out.println("Request Tenant ID: " + e.getTenantId());
        System.out.println("Approved By: " + p.uid());
        System.out.println("================================");

        if (e.getStatus() != TenantDeletionStatus.PENDING) {
            throw new IllegalStateException("Ticket is not PENDING");
        }

        System.out.println("About to update buildings status from PENDING_DELETION to DELETING");
        updateBuildingsStatus(e.getTenantId(), BuildingStatus.PENDING_DELETION, BuildingStatus.DELETING);
        

        changeStatusOfUnits(e.getTenantId());


        System.out.println("=== About to createBuildingDeletionRequests ===");
        System.out.println("Tenant ID: " + e.getTenantId());
        System.out.println("Approved By: " + p.uid());
        createBuildingDeletionRequests(e.getTenantId(), p.uid());
        System.out.println("=== Finished createBuildingDeletionRequests ===");
        e.setApprovedBy(p.uid());
        e.setNote(note);
        e.setApprovedAt(OffsetDateTime.now());
        e.setStatus(TenantDeletionStatus.APPROVED);
        repo.save(e);
        
        return toDTO(e);
    }


}
