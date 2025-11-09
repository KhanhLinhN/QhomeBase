package com.QhomeBase.assetmaintenanceservice.controller;

import com.QhomeBase.assetmaintenanceservice.dto.service.ResidentServiceDetailDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ResidentServiceSummaryDto;
import com.QhomeBase.assetmaintenanceservice.dto.service.ServiceCategoryDto;
import com.QhomeBase.assetmaintenanceservice.service.ResidentServiceCatalogService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/asset-maintenance/resident")
@RequiredArgsConstructor
@Slf4j
public class ResidentServiceCatalogController {

    private final ResidentServiceCatalogService residentServiceCatalogService;

    @GetMapping("/categories")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServiceCategoryDto>> getActiveCategories() {
        return ResponseEntity.ok(residentServiceCatalogService.getActiveCategories());
    }

    @GetMapping("/categories/{categoryCode}/services")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ResidentServiceSummaryDto>> getServicesByCategory(
            @PathVariable @NotBlank String categoryCode) {
        return ResponseEntity.ok(residentServiceCatalogService.getActiveServicesByCategoryCode(categoryCode));
    }

    @GetMapping("/services/{serviceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResidentServiceDetailDto> getServiceDetail(@PathVariable UUID serviceId) {
        return ResponseEntity.ok(residentServiceCatalogService.getServiceDetail(serviceId));
    }
}

