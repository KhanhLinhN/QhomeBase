package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.*;
import com.QhomeBase.baseservice.service.AssetInspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/asset-inspections")
@RequiredArgsConstructor
public class AssetInspectionController {

    private final AssetInspectionService inspectionService;

    @PostMapping
    public ResponseEntity<AssetInspectionDto> createInspection(
            @RequestBody CreateAssetInspectionRequest request) {
        AssetInspectionDto result = inspectionService.createInspection(request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/contract/{contractId}")
    public ResponseEntity<AssetInspectionDto> getInspectionByContractId(@PathVariable UUID contractId) {
        AssetInspectionDto result = inspectionService.getInspectionByContractId(contractId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<AssetInspectionItemDto> updateInspectionItem(
            @PathVariable UUID itemId,
            @RequestBody UpdateAssetInspectionItemRequest request) {
        AssetInspectionItemDto result = inspectionService.updateInspectionItem(itemId, request, null);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{inspectionId}/start")
    public ResponseEntity<AssetInspectionDto> startInspection(@PathVariable UUID inspectionId) {
        AssetInspectionDto result = inspectionService.startInspection(inspectionId, null);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{inspectionId}/complete")
    public ResponseEntity<AssetInspectionDto> completeInspection(
            @PathVariable UUID inspectionId,
            @RequestBody(required = false) String inspectorNotes) {
        AssetInspectionDto result = inspectionService.completeInspection(inspectionId, inspectorNotes, null);
        return ResponseEntity.ok(result);
    }
}

