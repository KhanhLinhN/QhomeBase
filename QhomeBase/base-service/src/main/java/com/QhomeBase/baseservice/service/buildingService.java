package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.model.building;
import com.QhomeBase.baseservice.repository.buildingRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@Transactional
public class buildingService {
    private buildingRepository respo;
    public buildingService(buildingRepository respo) {
        this.respo = respo;
    }
    public List<building> findAllByTenantIdOrderByCodeAsc(UUID tenantId) {
        return respo.findAllByTenantIdOrderByCodeAsc(tenantId).stream()
                .sorted(Comparator.comparing(building:: getId)).collect(Collectors.toList());
    }

}
