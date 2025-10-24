package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.client.WebClientService;
import com.QhomeBase.financebillingservice.dto.BuildingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final WebClientService webClientService;

    public List<BuildingDto> getAllBuildings(UUID tenantId) throws Exception {
        return webClientService.getAllBuildings(tenantId).collectList().block();
    }
}
