package com.QhomeBase.baseservice.service;

import com.QhomeBase.baseservice.client.ContractClient;
import com.QhomeBase.baseservice.client.FinanceBillingClient;
import com.QhomeBase.baseservice.dto.*;
import com.QhomeBase.baseservice.dto.finance.CreateInvoiceLineRequest;
import com.QhomeBase.baseservice.dto.finance.CreateInvoiceRequest;
import com.QhomeBase.baseservice.model.*;
import com.QhomeBase.baseservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final FinanceBillingClient financeBillingClient;
    private final HouseholdService householdService;
    private final ContractClient contractClient;

    @Transactional
    public AssetInspectionDto createInspection(CreateAssetInspectionRequest request, UUID createdBy) {
        inspectionRepository.findByContractId(request.contractId())
                .ifPresent(inspection -> {
                    throw new IllegalArgumentException("Inspection already exists for contract: " + request.contractId());
                });

        ContractDetailDto contract = contractClient.getContractById(request.contractId())
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + request.contractId()));

        // Allow creating inspection for EXPIRED or CANCELLED contracts
        // Note: When called from inter-service (data-docs-service during contract cancellation),
        // the contract status might still appear as ACTIVE due to transaction isolation/caching.
        // Since this endpoint is only accessible via permitAll (inter-service calls),
        // we allow creating inspection regardless of status to avoid race conditions.
        String contractStatus = contract.status();
        if (!"EXPIRED".equalsIgnoreCase(contractStatus) 
                && !"CANCELLED".equalsIgnoreCase(contractStatus)
                && !"ACTIVE".equalsIgnoreCase(contractStatus)) {
            // Only reject if status is something other than ACTIVE, EXPIRED, or CANCELLED
            // (e.g., PENDING, INACTIVE, etc.)
            throw new IllegalArgumentException("Can only create inspection for active, expired, or cancelled contracts. Contract status: " + contractStatus);
        }
        
        // Log if creating inspection for ACTIVE contract (likely being cancelled)
        if ("ACTIVE".equalsIgnoreCase(contractStatus)) {
            log.info("Creating inspection for ACTIVE contract {} (likely being cancelled by data-docs-service)", request.contractId());
        }

        Unit unit = unitRepository.findById(request.unitId())
                .orElseThrow(() -> new IllegalArgumentException("Unit not found: " + request.unitId()));

        LocalDate inspectionDate = request.inspectionDate();
        if (inspectionDate == null) {
            inspectionDate = contract.endDate();
            if (inspectionDate == null) {
                throw new IllegalArgumentException("Contract has no end date. Please specify inspection date.");
            }
        }

        // When contract is cancelled, the selected date is stored in inspectionDate
        // scheduledDate is optional and can be set later by staff
        // If scheduledDate is provided, use it; otherwise, set to null (can be updated later)
        LocalDate scheduledDate = request.scheduledDate();
        // Note: For cancelled contracts, the selected date is already in inspectionDate
        // scheduledDate can remain null and be set later by staff when scheduling the actual inspection

        AssetInspection inspection = AssetInspection.builder()
                .contractId(request.contractId())
                .unit(unit)
                .inspectionDate(inspectionDate)
                .scheduledDate(scheduledDate)
                .status(InspectionStatus.PENDING)
                .inspectorName(request.inspectorName())
                .inspectorId(request.inspectorId())
                .createdBy(createdBy)
                .build();

        log.info("Creating inspection with inspectorId: {} for contract: {}", request.inspectorId(), request.contractId());
        inspection = inspectionRepository.save(inspection);
        log.info("Created inspection: {} with inspectorId: {}", inspection.getId(), inspection.getInspectorId());

        List<Asset> assets = assetRepository.findByUnitId(request.unitId())
                .stream()
                .filter(Asset::getActive)
                .collect(Collectors.toList());

        log.info("Found {} active assets in unit {} for inspection {}", assets.size(), request.unitId(), inspection.getId());
        
        for (Asset asset : assets) {
            AssetInspectionItem item = AssetInspectionItem.builder()
                    .inspection(inspection)
                    .asset(asset)
                    .checked(false)
                    .build();
            inspectionItemRepository.save(item);
            log.debug("Created inspection item for asset: {} (type: {})", asset.getAssetCode(), asset.getAssetType());
        }

        log.info("Created asset inspection: {} for contract: {} with {} items", inspection.getId(), request.contractId(), assets.size());
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
        
        if (request.damageCost() != null && request.damageCost().compareTo(BigDecimal.ZERO) >= 0) {
            item.setDamageCost(request.damageCost());
        } else if (request.conditionStatus() != null) {
            BigDecimal damageCost = calculateDamageCost(item.getAsset(), request.conditionStatus());
            item.setDamageCost(damageCost);
        } else if (request.conditionStatus() == null && request.damageCost() == null && item.getConditionStatus() != null) {
            BigDecimal damageCost = calculateDamageCost(item.getAsset(), item.getConditionStatus());
            item.setDamageCost(damageCost);
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

        AssetInspection inspection = item.getInspection();
        updateTotalDamageCost(inspection);

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

        // Update total damage cost before completing
        updateTotalDamageCost(inspection);
        
        // Reload inspection to get the updated totalDamageCost
        inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));

        inspection.setStatus(InspectionStatus.COMPLETED);
        inspection.setInspectorNotes(inspectorNotes);
        inspection.setCompletedAt(OffsetDateTime.now());
        inspection.setCompletedBy(userId);
        inspection = inspectionRepository.save(inspection);

        log.info("Completed inspection: {} with total damage cost: {}", inspectionId, inspection.getTotalDamageCost());
        
        return toDto(inspection);
    }

    @Transactional(readOnly = true)
    public List<AssetInspectionDto> getAllInspections(UUID inspectorId, InspectionStatus status) {
        List<AssetInspection> inspections;
        
        if (inspectorId != null && status != null) {
            inspections = inspectionRepository.findByInspectorIdAndStatus(inspectorId, status);
        } else if (inspectorId != null) {
            inspections = inspectionRepository.findByInspectorId(inspectorId);
        } else if (status != null) {
            inspections = inspectionRepository.findByStatus(status);
        } else {
            inspections = inspectionRepository.findAll();
        }
        
        return inspections.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetInspectionDto> getInspectionsByTechnicianId(UUID technicianId) {
        List<AssetInspection> inspections = inspectionRepository.findByInspectorId(technicianId);
        return inspections.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetInspectionDto> getMyAssignments(UUID userId) {
        if (userId == null) {
            log.warn("getMyAssignments called with null userId");
            return java.util.Collections.emptyList();
        }
        
        List<InspectionStatus> activeStatuses = List.of(
                InspectionStatus.PENDING,
                InspectionStatus.IN_PROGRESS
        );
        
        try {
            log.info("Getting assignments for userId: {}", userId);
            List<AssetInspection> inspections = inspectionRepository.findByInspectorIdAndStatusIn(userId, activeStatuses);
            log.info("Found {} inspections for userId: {}", inspections.size(), userId);
            
            if (!inspections.isEmpty()) {
                inspections.forEach(ins -> log.debug("Inspection {} has inspectorId: {}", ins.getId(), ins.getInspectorId()));
            }
            
            return inspections.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Error getting assignments for userId: {}", userId, ex);
            throw new RuntimeException("Failed to get assignments: " + ex.getMessage(), ex);
        }
    }

    private AssetInspectionDto toDto(AssetInspection inspection) {
        List<AssetInspectionItem> items = inspectionItemRepository.findByInspectionIdWithAsset(inspection.getId());
        log.info("Loading {} items for inspection: {}", items.size(), inspection.getId());
        
        return new AssetInspectionDto(
                inspection.getId(),
                inspection.getContractId(),
                inspection.getUnit() != null ? inspection.getUnit().getId() : null,
                inspection.getUnit() != null ? inspection.getUnit().getCode() : null,
                inspection.getInspectionDate(),
                inspection.getScheduledDate(),
                inspection.getStatus(),
                inspection.getInspectorName(),
                inspection.getInspectorId(),
                inspection.getInspectorNotes(),
                inspection.getCompletedAt(),
                inspection.getCompletedBy(),
                inspection.getCreatedAt(),
                inspection.getUpdatedAt(),
                items.stream().map(this::toItemDto).collect(Collectors.toList()),
                inspection.getTotalDamageCost() != null ? inspection.getTotalDamageCost() : BigDecimal.ZERO,
                inspection.getInvoiceId()
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
                item.getCheckedBy(),
                item.getDamageCost() != null ? item.getDamageCost() : BigDecimal.ZERO,
                asset != null && asset.getPurchasePrice() != null ? asset.getPurchasePrice() : BigDecimal.ZERO
        );
    }

    private BigDecimal calculateDamageCost(Asset asset, String conditionStatus) {
        if (asset == null || asset.getPurchasePrice() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal purchasePrice = asset.getPurchasePrice();
        
        if (conditionStatus == null || conditionStatus.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        String status = conditionStatus.toUpperCase().trim();
        
        switch (status) {
            case "GOOD":
                return BigDecimal.ZERO;
            case "DAMAGED":
                return purchasePrice.multiply(new BigDecimal("0.5"));
            case "MISSING":
                return purchasePrice;
            default:
                log.warn("Unknown condition status: {}, returning 0", conditionStatus);
                return BigDecimal.ZERO;
        }
    }

    private void updateTotalDamageCost(AssetInspection inspection) {
        List<AssetInspectionItem> items = inspectionItemRepository.findByInspectionId(inspection.getId());
        log.debug("Calculating total damage cost for inspection {} with {} items", inspection.getId(), items.size());
        
        BigDecimal total = items.stream()
                .map(item -> {
                    BigDecimal damageCost = item.getDamageCost() != null ? item.getDamageCost() : BigDecimal.ZERO;
                    log.debug("Item {} has damageCost: {}", item.getId(), damageCost);
                    return damageCost;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        inspection.setTotalDamageCost(total);
        inspectionRepository.save(inspection);
        
        log.info("Updated total damage cost for inspection {}: {} (from {} items)", inspection.getId(), total, items.size());
    }

    @Transactional
    public AssetInspectionDto recalculateDamageCost(UUID inspectionId) {
        AssetInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));
        
        updateTotalDamageCost(inspection);
        return toDto(inspection);
    }

    /**
     * Update scheduled date for inspection
     * Allows multiple updates (no restriction on how many times)
     */
    @Transactional
    public AssetInspectionDto updateScheduledDate(UUID inspectionId, LocalDate scheduledDate) {
        AssetInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));
        
        if (scheduledDate == null) {
            throw new IllegalArgumentException("Scheduled date cannot be null");
        }
        
        // Allow updating scheduledDate multiple times - no restriction
        inspection.setScheduledDate(scheduledDate);
        inspection = inspectionRepository.save(inspection);
        
        log.info("Updated scheduled date for inspection {} to {}", inspectionId, scheduledDate);
        return toDto(inspection);
    }

    @Transactional
    public AssetInspectionDto assignInspector(UUID inspectionId, UUID inspectorId, String inspectorName) {
        AssetInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));
        
        if (inspectorId == null) {
            throw new IllegalArgumentException("Inspector ID cannot be null");
        }
        
        inspection.setInspectorId(inspectorId);
        inspection.setInspectorName(inspectorName);
        inspection = inspectionRepository.save(inspection);
        
        log.info("Assigned inspector {} ({}) to inspection {}", inspectorName, inspectorId, inspectionId);
        return toDto(inspection);
    }

   
    @Transactional
    public AssetInspectionDto generateInvoice(UUID inspectionId, UUID createdBy) {
        AssetInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));
        
        if (inspection.getStatus() != InspectionStatus.COMPLETED) {
            throw new IllegalArgumentException("Can only generate invoice for completed inspections");
        }
        
        // Recalculate total damage cost before generating invoice to ensure accuracy
        updateTotalDamageCost(inspection);
        inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));
        
        if (inspection.getTotalDamageCost() == null || inspection.getTotalDamageCost().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Cannot generate invoice for inspection {}: totalDamageCost is {} (null or zero)", 
                    inspectionId, inspection.getTotalDamageCost());
            throw new IllegalArgumentException("No damage cost to invoice. Total damage cost is zero.");
        }
        
        if (inspection.getInvoiceId() != null) {
            throw new IllegalArgumentException("Invoice already generated for this inspection. Invoice ID: " + inspection.getInvoiceId());
        }
        
        Unit unit = inspection.getUnit();
        if (unit == null) {
            throw new IllegalArgumentException("Unit not found for inspection: " + inspectionId);
        }

        UUID unitId = unit.getId();

        UUID payerResidentId = householdService.getPayerForUnit(unitId);
        if (payerResidentId == null) {
            log.warn("No primary resident found for unit: {}. Proceeding with null payerResidentId.", unitId);
        }

        String unitCode = unit.getCode() != null ? unit.getCode() : "";
        String billToName = String.format("Căn hộ %s", unitCode);
        BigDecimal totalDamageCost = inspection.getTotalDamageCost();

        String description = String.format("Tiền thiệt hại thiết bị - Kiểm tra ngày %s", 
                inspection.getInspectionDate().toString());

        CreateInvoiceLineRequest invoiceLine = CreateInvoiceLineRequest.builder()
                .serviceDate(inspection.getInspectionDate())
                .description(description)
                .quantity(BigDecimal.ONE)
                .unit("lần")
                .unitPrice(totalDamageCost)
                .taxRate(BigDecimal.ZERO)
                .serviceCode("ASSET_DAMAGE")
                .externalRefType("ASSET_INSPECTION")
                .externalRefId(inspectionId)
                .build();

        // Get water and electricity invoices for this unit
        List<CreateInvoiceLineRequest> invoiceLines = new java.util.ArrayList<>(List.of(invoiceLine));
        
        try {
            log.info("Attempting to get invoices for unit {} when generating asset inspection invoice", unitId);
            List<com.QhomeBase.baseservice.dto.finance.InvoiceDto> unitInvoices = 
                    financeBillingClient.getInvoicesByUnitSync(unitId);
            
            log.info("Found {} invoices for unit {} when generating asset inspection invoice", unitInvoices.size(), unitId);
            
            // Log raw invoice data to debug serialization issues
            if (unitInvoices.isEmpty()) {
                log.warn("No invoices found for unit {} - this might be expected if water/electric invoices haven't been created yet", unitId);
            }
            
            // Log all invoice details for debugging
            for (com.QhomeBase.baseservice.dto.finance.InvoiceDto inv : unitInvoices) {
                if (inv.getLines() != null && !inv.getLines().isEmpty()) {
                    List<String> serviceCodes = new java.util.ArrayList<>();
                    for (com.QhomeBase.baseservice.dto.finance.InvoiceLineDto line : inv.getLines()) {
                        if (line != null) {
                            String code = line.getServiceCode();
                            if (code != null) {
                                serviceCodes.add(code);
                            } else {
                                log.warn("Invoice {} has line with null serviceCode. lineId={}, description={}", 
                                        inv.getId(), line.getId(), line.getDescription());
                            }
                        }
                    }
                    log.info("Invoice {}: status={}, serviceCodes={}, totalAmount={}, linesCount={}", 
                            inv.getId(), inv.getStatus(), serviceCodes, inv.getTotalAmount(), inv.getLines().size());
                } else {
                    log.warn("Invoice {} has no lines or lines is null. status={}, totalAmount={}", 
                            inv.getId(), inv.getStatus(), inv.getTotalAmount());
                }
            }
            
            // Filter for WATER and ELECTRIC invoices that are PUBLISHED or PAID
            int waterElectricCount = 0;
            for (com.QhomeBase.baseservice.dto.finance.InvoiceDto invoice : unitInvoices) {
                if (invoice.getLines() != null && !invoice.getLines().isEmpty()) {
                    // Check if invoice has WATER or ELECTRIC service code
                    boolean hasWaterOrElectric = invoice.getLines().stream()
                            .filter(line -> line != null && line.getServiceCode() != null)
                            .anyMatch(line -> "WATER".equals(line.getServiceCode()) || 
                                            "ELECTRIC".equals(line.getServiceCode()));
                    
                    log.info("Invoice {} has status: {}, hasWaterOrElectric: {}, linesCount: {}", 
                            invoice.getId(), invoice.getStatus(), hasWaterOrElectric, invoice.getLines().size());
                    
                    // Include PUBLISHED or PAID invoices (PAID invoices might have been paid but we still want to include them in the combined invoice)
                    // This ensures we capture water/electric costs even if invoices were already paid
                    if (hasWaterOrElectric && ("PUBLISHED".equals(invoice.getStatus()) || "PAID".equals(invoice.getStatus()))) {
                        // Get the first line with WATER or ELECTRIC service code
                        com.QhomeBase.baseservice.dto.finance.InvoiceLineDto firstLine = invoice.getLines().stream()
                                .filter(line -> line != null && line.getServiceCode() != null)
                                .filter(line -> "WATER".equals(line.getServiceCode()) || "ELECTRIC".equals(line.getServiceCode()))
                                .findFirst()
                                .orElse(null);
                        
                        if (firstLine == null) {
                            log.warn("Could not find WATER or ELECTRIC line in invoice {}", invoice.getId());
                            continue;
                        }
                        
                        String serviceCode = firstLine.getServiceCode();
                        
                        // Create invoice line for water/electricity
                        CreateInvoiceLineRequest waterElectricLine = CreateInvoiceLineRequest.builder()
                                .serviceDate(firstLine.getServiceDate() != null ? firstLine.getServiceDate() : inspection.getInspectionDate())
                                .description(String.format("Tiền %s - %s", 
                                        "WATER".equals(serviceCode) ? "nước" : "điện",
                                        firstLine.getDescription() != null ? firstLine.getDescription() : ""))
                                .quantity(BigDecimal.ONE)
                                .unit("hóa đơn")
                                .unitPrice(invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO)
                                .taxRate(BigDecimal.ZERO)
                                .serviceCode(serviceCode)
                                .externalRefType("WATER_ELECTRIC_INVOICE")
                                .externalRefId(invoice.getId())
                                .build();
                        
                        invoiceLines.add(waterElectricLine);
                        waterElectricCount++;
                        log.info("Including {} invoice {} (amount: {}) in asset inspection invoice", 
                                serviceCode, invoice.getId(), invoice.getTotalAmount());
                    }
                }
            }
            
            if (waterElectricCount == 0) {
                log.warn("No water/electric invoices found for unit {} when generating asset inspection invoice. " +
                        "This might be because invoices haven't been created yet from meter readings.", unitId);
            } else {
                log.info("Added {} water/electric invoice lines to asset inspection invoice", waterElectricCount);
            }
        } catch (Exception e) {
            log.warn("Failed to get water/electricity invoices for unit {}: {}. Proceeding with damage cost only.", 
                    unitId, e.getMessage(), e);
        }

        CreateInvoiceRequest invoiceRequest = CreateInvoiceRequest.builder()
                .dueDate(LocalDate.now().plusDays(7))
                .currency("VND")
                .billToName(billToName)
                .billToAddress(null)
                .billToContact(null)
                .payerUnitId(unitId)
                .payerResidentId(payerResidentId)
                .cycleId(null)
                .status("PAID") 
                .lines(invoiceLines)
                .build();
        try {
            com.QhomeBase.baseservice.dto.finance.InvoiceDto invoiceDto = financeBillingClient.createInvoiceSync(invoiceRequest);
            
            UUID invoiceId = invoiceDto.getId();
            inspection.setInvoiceId(invoiceId);
            inspection = inspectionRepository.save(inspection);
            
            log.info("Generated invoice {} for inspection {} with total damage cost: {}", 
                    invoiceId, inspectionId, inspection.getTotalDamageCost());
            
            return toDto(inspection);
        } catch (Exception e) {
            log.error("Failed to create invoice in finance-billing-service for inspection: {}", inspectionId, e);
            throw new RuntimeException("Failed to create invoice: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<AssetInspectionDto> getInspectionsPendingApproval() {
        List<AssetInspection> inspections = inspectionRepository.findCompletedInspectionsPendingApproval(InspectionStatus.COMPLETED);
        List<AssetInspection> filteredInspections = inspections.stream()
                .filter(ai -> ai.getTotalDamageCost() != null 
                        && ai.getTotalDamageCost().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        log.info("Found {} inspections pending approval (filtered from {} total)", 
                filteredInspections.size(), inspections.size());
        return filteredInspections.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public AssetInspectionDto approveInspection(UUID inspectionId, UUID approvedBy) {
        AssetInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));

        if (inspection.getStatus() != InspectionStatus.COMPLETED) {
            throw new IllegalArgumentException("Can only approve completed inspections");
        }

        if (inspection.getTotalDamageCost() == null || inspection.getTotalDamageCost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cannot approve inspection with no damage cost");
        }

        if (inspection.getInvoiceId() != null) {
            throw new IllegalArgumentException("Invoice already generated for this inspection");
        }

        log.info("Admin {} approving inspection {} and generating invoice", approvedBy, inspectionId);
        return generateInvoice(inspectionId, approvedBy);
    }

    @Transactional
    public AssetInspectionDto rejectInspection(UUID inspectionId, String rejectionNotes, UUID rejectedBy) {
        AssetInspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found: " + inspectionId));

        if (inspection.getStatus() != InspectionStatus.COMPLETED) {
            throw new IllegalArgumentException("Can only reject completed inspections");
        }

        if (inspection.getInvoiceId() != null) {
            throw new IllegalArgumentException("Cannot reject inspection that already has an invoice");
        }

        String currentNotes = inspection.getInspectorNotes() != null ? inspection.getInspectorNotes() : "";
        String rejectionMessage = String.format("\n\n[Admin rejection - %s]: %s", 
                java.time.OffsetDateTime.now().toString(), 
                rejectionNotes != null ? rejectionNotes : "Rejected by admin");
        inspection.setInspectorNotes(currentNotes + rejectionMessage);
        
        inspection = inspectionRepository.save(inspection);
        log.info("Admin {} rejected inspection {}: {}", rejectedBy, inspectionId, rejectionNotes);
        
        return toDto(inspection);
    }


    @Transactional
    public int createInspectionsForExpiredContracts(LocalDate endOfMonth) {
        log.info("Creating inspections for contracts expired in month ending: {}", endOfMonth);
        
        int createdCount = 0;
        
        List<Unit> allUnits = unitRepository.findAll();
        log.info("Checking {} units for expired contracts", allUnits.size());
        
        for (Unit unit : allUnits) {
            try {
                List<ContractSummary> contracts = contractClient.getContractsByUnit(unit.getId());
                
                for (ContractSummary contract : contracts) {
                    if (contract.endDate() == null) {
                        continue;
                    }
                    
                    LocalDate contractEndDate = contract.endDate();
                    boolean expiredInMonth = contractEndDate.getYear() == endOfMonth.getYear() &&
                                            contractEndDate.getMonth() == endOfMonth.getMonth();
                    
                    if (!expiredInMonth) {
                        continue;
                    }
                    
                    if (inspectionRepository.findByContractId(contract.id()).isPresent()) {
                        log.debug("Inspection already exists for contract: {}", contract.id());
                        continue;
                    }
                    
                    if (!"EXPIRED".equalsIgnoreCase(contract.status())) {
                        continue;
                    }
                    
                    try {
                        LocalDate inspectionDate = contractEndDate;
                        // scheduledDate = null sẽ được set thành endDate trong createInspection
                        CreateAssetInspectionRequest request = new CreateAssetInspectionRequest(
                                contract.id(),
                                unit.getId(),
                                inspectionDate,
                                null, // scheduledDate = null -> sẽ dùng endDate của contract
                                null, // inspectorName
                                null  // inspectorId
                        );
                        
                        createInspection(request, null);
                        createdCount++;
                        log.info("Created automatic inspection for expired contract: {} in unit: {}", 
                                contract.id(), unit.getCode());
                    } catch (IllegalArgumentException e) {
                        log.debug("Skipping contract {}: {}", contract.id(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Error processing unit {} for expired contracts: {}", unit.getId(), e.getMessage(), e);
            }
        }
        
        log.info("Created {} automatic inspections for expired contracts ending in month: {}", createdCount, endOfMonth);
        return createdCount;
    }
}

