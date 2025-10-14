package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.BuildingDto;
import com.QhomeBase.baseservice.dto.BuildingUpdateReq;
import com.QhomeBase.baseservice.service.buildingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/buildings")
@RequiredArgsConstructor
public class BuildingUpdateController {
    
    private final buildingService buildingService;
    
    @PutMapping("/{buildingId}")
    @PreAuthorize("@authz.canUpdateBuilding()")
    public ResponseEntity<BuildingDto> updateBuilding(
            @Valid @RequestBody BuildingUpdateReq req,
            Authentication auth) {
        try {
            BuildingDto updatedBuilding = buildingService.updateBuilding(req, auth);
            return ResponseEntity.ok(updatedBuilding);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
