package com.QhomeBase.financebillingservice.service;

import com.QhomeBase.financebillingservice.client.WebClientService;
import com.QhomeBase.financebillingservice.dto.BuildingDto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class tenantService {
    private final WebClientService webClientService;
    public tenantService(WebClientService webClientService) {
        this.webClientService = webClientService;
    }
    public List<BuildingDto> getAllBuildings(UUID tenantId) throws Exception{
        List<BuildingDto> buildingDtos = new ArrayList<>();
        buildingDtos = webClientService.getAllBuildings(tenantId).collectList().block();
        return buildingDtos;
    }

}
