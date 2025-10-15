package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.BuildingCreateReq;
import com.QhomeBase.baseservice.dto.BuildingDto;
import com.QhomeBase.baseservice.service.BuildingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/buildings")
@RequiredArgsConstructor
public class BuildingCreateController {
    private final BuildingService buildingService;
    @PostMapping
    @PreAuthorize(("@authz.canCreateBuilding()"))
    public BuildingDto createBuilding(@Valid @RequestBody BuildingCreateReq req, Authentication auth) {
        return buildingService.createBuilding(req, auth);
    }

}
