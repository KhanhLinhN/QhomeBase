package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.dto.*;
import com.QhomeBase.baseservice.model.*;
import com.QhomeBase.baseservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetInspectionService {

    private final AssetInspectionRepository inspectionRepository;
    private final AssetInspectionItemRepository inspectionItemRepository;
    private final AssetRepository assetRepository;
    private final UnitRepository unitRepository;

    @Transactional
    public AssetInspectionDto createInspection(CreateAssetInspectionRequest request, UUID createdBy) {
        // Check if inspection already exists for this contract
        inspectionRepository.findByContractId(request.contractId())
                .ifPresent(inspection -> {
                    throw new IllegalArgumentException("Inspection already exists for contract: " + request.contractId());
                });

        // Get unit
        Unit unit = unitRepository.findById(request.unitId())
                .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + request.unitId()));

        // Create inspection
        AssetInspection inspection = AssetInspection.builder()
                .contractId(request.contractId())
                .unit(unit)
                .inspectionDate(request.inspectionDate())
                .status(InspectionStatus.PENDING)
                .inspectorName(request.inspectorName())
                .createdBy(createdBy)
                .build();

        inspection = inspectionRepository.save(inspection);

        // Create inspection items for all active assets in the unit
        List<Asset> assets = assetRepository.findByUnitId(request.unitId())
                .stream()
                .filter(Asset::getActive)
                .collect(Collectors.toList());

        for (Asset asset : assets) {
            AssetInspectionItem item = AssetInspectionItem.builder()
                    .inspection(inspection)
                    .asset(asset)
                    .checked(false)
                    .build();
            inspectionItemRepository.save(item);
        }

        log.info("Created asset inspection: {} for contract: {}", inspection.getId(), request.contractId());
        return toDto(inspection);
    }

    @Transactional(readOnly = true)
    public AssetInspectionDto getInspectionByContractId(UUID contractId) {
        AssetInspection inspection = inspectionRepository.findByContractId(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found for contract: " + contractId));
        return toDto(inspection);
    }

    @Transactional
    public AssetInspectionItemDto updateInspectionItem(UUID itemId, UpdateAssetInspectionItemRequest request, UUID checkedBy) {
        AssetInspectionItem item = inspectionItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection item not found: " + itemId));

        if (request.conditionStatus() != null) {
            item.setConditionStatus(request.conditionStatus());
        }
        if (request.notes() != null) {
            item.setNotes(request.notes());
        }
        if (request.checked() != null) {
            item.setChecked(request.checked());
            if (request.checked()) {
                item.setCheckedAt(OffsetDateTime.now());
                item.setCheckedBy(checkedBy);
            } else {
                item.setCheckedAt(null);
                item.setCheckedBy(null);
            }
        }

        item = inspectionItemRepository.save(item);

        // Update inspection status if all items are checked
        AssetInspection inspection = item.getInspection();
        List<AssetInspectionItem> allItems = inspectionItemRepository.findByInspectionId(inspection.getId());
        boolean allChecked = allItems.stream().allMatch(AssetInspectionItem::getChecked);
        if (allChecked && inspection.getStatus() == InspectionStatus.IN_PROGRESS) {
            inspection.setStatus(InspectionStatus.COMPLETED);
            inspection.setCompletedAt(OffsetDateTime.now());
            inspection.setCompletedBy(checkedBy);
            inspectionRepository.save(inspection);
        }

        return toItemDto(item);
    }

    @Transactional
    public AssetInspectionDto startInspection(UUID inspectionId, UUID userId) {
        AssetInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));

        if (inspection.getStatus() != InspectionStatus.PENDING) {
            throw new IllegalArgumentException("Inspection is not in PENDING status");
        }

        inspection.setStatus(InspectionStatus.IN_PROGRESS);
        inspection = inspectionRepository.save(inspection);

        log.info("Started inspection: {}", inspectionId);
        return toDto(inspection);
    }

    @Transactional
    public AssetInspectionDto completeInspection(UUID inspectionId, String inspectorNotes, UUID userId) {
        AssetInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));

        inspection.setStatus(InspectionStatus.COMPLETED);
        inspection.setInspectorNotes(inspectorNotes);
        inspection.setCompletedAt(OffsetDateTime.now());
        inspection.setCompletedBy(userId);
        inspection = inspectionRepository.save(inspection);

        log.info("Completed inspection: {}", inspectionId);
        return toDto(inspection);
    }

    private AssetInspectionDto toDto(AssetInspection inspection) {
        List<AssetInspectionItem> items = inspectionItemRepository.findByInspectionId(inspection.getId());
        
        return new AssetInspectionDto(
                inspection.getId(),
                inspection.getContractId(),
                inspection.getUnit() != null ? inspection.getUnit().getId() : null,
                inspection.getUnit() != null ? inspection.getUnit().getCode() : null,
                inspection.getInspectionDate(),
                inspection.getStatus(),
                inspection.getInspectorName(),
                inspection.getInspectorNotes(),
                inspection.getCompletedAt(),
                inspection.getCompletedBy(),
                inspection.getCreatedAt(),
                inspection.getUpdatedAt(),
                items.stream().map(this::toItemDto).collect(Collectors.toList())
        );
    }

    private AssetInspectionItemDto toItemDto(AssetInspectionItem item) {
        Asset asset = item.getAsset();
        return new AssetInspectionItemDto(
                item.getId(),
                asset != null ? asset.getId() : null,
                asset != null ? asset.getAssetCode() : null,
                asset != null ? asset.getName() : null,
                asset != null ? asset.getAssetType().name() : null,
                item.getConditionStatus(),
                item.getNotes(),
                item.getChecked(),
                item.getCheckedAt(),
                item.getCheckedBy()
        );
    }
}

