package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.model.BuildingStatus;
import com.QhomeBase.baseservice.model.building;
import com.QhomeBase.baseservice.repository.buildingRepository;
import com.QhomeBase.baseservice.repository.tenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantDeletionService {
    private final buildingRepository buildingRepository;
    private final tenantRepository tenantRepository;
    public void changeStatusOfBuilding(UUID tenantid) {
        List<building> buildings = buildingRepository.findAllByTenantIdOrderByCodeAsc(tenantid);
        for (building building : buildings) {
            if (building.getStatus().equals(BuildingStatus.ACTIVE)) {
                building.setStatus(BuildingStatus.PENDING_DELETION);
            }
        }
    }

}
