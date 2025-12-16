package com.QhomeBase.datadocsservice.service;

import com.QhomeBase.datadocsservice.client.BaseServiceClient;
import com.QhomeBase.datadocsservice.client.InvoiceClient;
import com.QhomeBase.datadocsservice.config.VnpayProperties;
import com.QhomeBase.datadocsservice.dto.*;
import com.QhomeBase.datadocsservice.model.Contract;
import com.QhomeBase.datadocsservice.model.ContractFile;
import com.QhomeBase.datadocsservice.repository.ContractFileRepository;
import com.QhomeBase.datadocsservice.repository.ContractRepository;
import com.QhomeBase.datadocsservice.service.vnpay.VnpayService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractFileRepository contractFileRepository;
    private final FileStorageService fileStorageService;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;
    private final InvoiceClient invoiceClient;
    private final BaseServiceClient baseServiceClient;
    private final EntityManager entityManager;

    @Transactional
    public ContractDto createContract(CreateContractRequest request, UUID createdBy) {
        contractRepository.findByContractNumber(request.getContractNumber())
                .ifPresent(contract -> {
                    throw new IllegalArgumentException("Contract number already exists: " + request.getContractNumber());
                });

        String contractType = request.getContractType() != null ? request.getContractType() : "RENTAL";
        
        if (!"RENTAL".equals(contractType) && !"PURCHASE".equals(contractType)) {
            throw new IllegalArgumentException("Invalid contract type. Must be RENTAL or PURCHASE");
        }

        if ("RENTAL".equals(contractType)) {
            if (request.getEndDate() != null && request.getStartDate().isAfter(request.getEndDate())) {
                throw new IllegalArgumentException("Start date must be before or equal to end date");
            }
            if (request.getMonthlyRent() == null) {
                throw new IllegalArgumentException("Monthly rent is required for RENTAL contracts");
            }
        } else if ("PURCHASE".equals(contractType)) {
            if (request.getEndDate() != null) {
                throw new IllegalArgumentException("Purchase contracts cannot have end date");
            }
            if (request.getPurchasePrice() == null) {
                throw new IllegalArgumentException("Purchase price is required for PURCHASE contracts");
            }
            if (request.getPurchaseDate() == null) {
                throw new IllegalArgumentException("Purchase date is required for PURCHASE contracts");
            }
            if (request.getPaymentMethod() != null || request.getPaymentTerms() != null) {
                throw new IllegalArgumentException("Purchase contracts are fully paid. Payment method and terms are not applicable");
            }
        }
        
        Contract contract = Contract.builder()
                .unitId(request.getUnitId())
                .contractNumber(request.getContractNumber())
                .contractType(contractType)
                .startDate(request.getStartDate())
                .endDate("RENTAL".equals(contractType) ? request.getEndDate() : null)
                .monthlyRent("RENTAL".equals(contractType) ? request.getMonthlyRent() : null)
                .purchasePrice("PURCHASE".equals(contractType) ? request.getPurchasePrice() : null)
                .purchaseDate("PURCHASE".equals(contractType) ? request.getPurchaseDate() : null)
                .notes(request.getNotes())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .createdBy(createdBy)
                .build();

        contract = contractRepository.save(contract);
        log.info("Created contract: {} for unit: {}", contract.getId(), request.getUnitId());

        return toDto(contract);
    }

    @Transactional
    public ContractDto updateContract(UUID contractId, UpdateContractRequest request, UUID updatedBy) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

        if (request.getContractNumber() != null && !request.getContractNumber().equals(contract.getContractNumber())) {
            contractRepository.findByContractNumber(request.getContractNumber())
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Contract number already exists: " + request.getContractNumber());
                    });
            contract.setContractNumber(request.getContractNumber());
        }
        
        if (request.getContractType() != null) {
            String newContractType = request.getContractType();
            if (!"RENTAL".equals(newContractType) && !"PURCHASE".equals(newContractType)) {
                throw new IllegalArgumentException("Invalid contract type. Must be RENTAL or PURCHASE");
            }
            
            String oldContractType = contract.getContractType();
            contract.setContractType(newContractType);
            
            if (!oldContractType.equals(newContractType)) {
                if ("RENTAL".equals(newContractType)) {
                    contract.setPurchasePrice(null);
                    contract.setPaymentMethod(null);
                    contract.setPaymentTerms(null);
                    contract.setPurchaseDate(null);
                } else if ("PURCHASE".equals(newContractType)) {
                    contract.setEndDate(null);
                }
            }
        }
        
        String currentType = contract.getContractType();
        
        if (request.getStartDate() != null) {
            contract.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            if ("PURCHASE".equals(currentType)) {
                throw new IllegalArgumentException("Purchase contracts cannot have end date");
            }
            contract.setEndDate(request.getEndDate());
        }
        if (request.getMonthlyRent() != null) {
            if ("PURCHASE".equals(currentType)) {
                throw new IllegalArgumentException("Purchase contracts cannot have monthly rent");
            }
            contract.setMonthlyRent(request.getMonthlyRent());
        }
        if (request.getPurchasePrice() != null) {
            if ("RENTAL".equals(currentType)) {
                throw new IllegalArgumentException("Rental contracts cannot have purchase price");
            }
            contract.setPurchasePrice(request.getPurchasePrice());
        }
        if (request.getPaymentMethod() != null || request.getPaymentTerms() != null) {
            if ("PURCHASE".equals(currentType)) {
                throw new IllegalArgumentException("Purchase contracts are fully paid. Payment method and terms are not applicable");
            }
            
            if (request.getPaymentMethod() != null) {
                contract.setPaymentMethod(request.getPaymentMethod());
            }
            if (request.getPaymentTerms() != null) {
                contract.setPaymentTerms(request.getPaymentTerms());
            }
        }
        if (request.getPurchaseDate() != null) {
            if ("RENTAL".equals(currentType)) {
                throw new IllegalArgumentException("Rental contracts cannot have purchase date");
            }
            contract.setPurchaseDate(request.getPurchaseDate());
        }
        if (request.getNotes() != null) {
            contract.setNotes(request.getNotes());
        }
        if (request.getStatus() != null) {
            contract.setStatus(request.getStatus());
        }

        if ("RENTAL".equals(currentType)) {
            if (contract.getEndDate() != null && contract.getStartDate().isAfter(contract.getEndDate())) {
                throw new IllegalArgumentException("Start date must be before or equal to end date");
            }
        } else if ("PURCHASE".equals(currentType)) {
            if (contract.getEndDate() != null) {
                throw new IllegalArgumentException("Purchase contracts cannot have end date");
            }
        }

        contract.setUpdatedBy(updatedBy);
        contract = contractRepository.save(contract);
        log.info("Updated contract: {}", contractId);

        return toDto(contract);
    }

    public ContractDto getContractById(UUID contractId) {
        Contract contract = contractRepository.findByIdWithFiles(contractId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Contract not found: " + contractId));
        return toDto(contract);
    }

    @Transactional(readOnly = true)
    public List<ContractDto> getContractsByUnitId(UUID unitId) {
        try {
            List<Contract> contracts = contractRepository.findByUnitId(unitId);
            return contracts.stream()
                    .map(contract -> {
                        try {
                            return toDto(contract);
                        } catch (Exception e) {
                            log.error("[ContractService] Lỗi khi convert contract {} sang DTO: {}", 
                                    contract.getId(), e.getMessage(), e);
                            return ContractDto.builder()
                                    .id(contract.getId())
                                    .unitId(contract.getUnitId())
                                    .contractNumber(contract.getContractNumber())
                                    .contractType(contract.getContractType())
                                    .startDate(contract.getStartDate())
                                    .endDate(contract.getEndDate())
                                    .monthlyRent(contract.getMonthlyRent())
                                    .purchasePrice(contract.getPurchasePrice())
                                    .paymentMethod(contract.getPaymentMethod())
                                    .paymentTerms(contract.getPaymentTerms())
                                    .purchaseDate(contract.getPurchaseDate())
                                    .notes(contract.getNotes())
                                    .status(contract.getStatus())
                                    .createdBy(contract.getCreatedBy())
                                    .createdAt(contract.getCreatedAt())
                                    .updatedAt(contract.getUpdatedAt())
                                    .updatedBy(contract.getUpdatedBy())
                                    .files(List.of())
                                    .build();
                        }
                    })
                    .sorted((c1, c2) -> {
                        // Sort by priority: ACTIVE → INACTIVE → CANCELLED → EXPIRED
                        int priority1 = getStatusPriority(c1.getStatus());
                        int priority2 = getStatusPriority(c2.getStatus());
                        if (priority1 != priority2) {
                            return Integer.compare(priority1, priority2);
                        }
                        // If same priority, sort by endDate (most recent first, nulls last)
                        if (c1.getEndDate() != null && c2.getEndDate() != null) {
                            return c2.getEndDate().compareTo(c1.getEndDate());
                        }
                        if (c1.getEndDate() != null) return -1;
                        if (c2.getEndDate() != null) return 1;
                        // If both null, sort by createdAt (most recent first)
                        if (c1.getCreatedAt() != null && c2.getCreatedAt() != null) {
                            return c2.getCreatedAt().compareTo(c1.getCreatedAt());
                        }
                        return 0;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("[ContractService] Lỗi khi lấy contracts cho unit {}: {}", unitId, e.getMessage(), e);
            throw new RuntimeException("Không thể lấy danh sách hợp đồng: " + e.getMessage(), e);
        }
    }

    /**
     * Get priority for contract status sorting
     * Lower number = higher priority
     * ACTIVE = 1 (highest priority)
     * INACTIVE = 2
     * CANCELLED = 3
     * EXPIRED = 4 (lowest priority)
     * Other statuses = 5
     */
    private int getStatusPriority(String status) {
        if (status == null) return 99;
        String upperStatus = status.toUpperCase();
        switch (upperStatus) {
            case "ACTIVE":
                return 1;
            case "INACTIVE":
                return 2;
            case "CANCELLED":
                return 3;
            case "EXPIRED":
                return 4;
            default:
                return 5;
        }
    }

    public List<ContractDto> getActiveContracts() {
        List<Contract> contracts = contractRepository.findActiveContracts(LocalDate.now());
        return contracts.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ContractDto> getActiveContractsByUnit(UUID unitId) {
        List<Contract> contracts = contractRepository.findActiveContractsByUnit(unitId, LocalDate.now());
        return contracts.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractDto> getAllContracts() {
        List<Contract> contracts = contractRepository.findAll();
        return contracts.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::toDtoSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractDto> getContractsByType(String contractType) {
        if (contractType == null || contractType.isEmpty()) {
            throw new IllegalArgumentException("Contract type is required");
        }
        String upperContractType = contractType.toUpperCase();
        List<Contract> allContracts = contractRepository.findAll();
        List<Contract> contracts = allContracts.stream()
                .filter(c -> upperContractType.equals(c.getContractType()))
                .collect(Collectors.toList());
        return contracts.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::toDtoSummary)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteContract(UUID contractId) {
        Contract contract = contractRepository.findByIdWithFiles(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

        contract.getFiles().forEach(file -> {
            if (!file.getIsDeleted()) {
                file.setIsDeleted(true);
                file.setDeletedAt(java.time.OffsetDateTime.now());
                contractFileRepository.save(file);
            }
        });

        contractRepository.delete(contract);
        log.info("Deleted contract: {}", contractId);
    }

    @Transactional
    public ContractFileDto uploadContractFile(UUID contractId, MultipartFile file, UUID uploadedBy, Boolean isPrimary) {
        Contract contract = contractRepository.findByIdWithFiles(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

        FileUploadResponse uploadResponse = fileStorageService.uploadContractFile(file, contractId, uploadedBy);

        if (Boolean.TRUE.equals(isPrimary)) {
            contractFileRepository.findPrimaryFileByContractId(contractId)
                    .ifPresent(primaryFile -> {
                        primaryFile.setIsPrimary(false);
                        contractFileRepository.save(primaryFile);
                    });
        } else {
            List<ContractFile> existingFiles = contractFileRepository.findByContractId(contractId);
            if (existingFiles.isEmpty()) {
                isPrimary = true;
            }
        }

        Integer displayOrder = contractFileRepository.findByContractId(contractId).size();
        ContractFile contractFile = ContractFile.builder()
                .contract(contract)
                .fileName(uploadResponse.getFileName())
                .originalFileName(uploadResponse.getOriginalFileName())
                .filePath("contracts/" + contractId + "/" + uploadResponse.getFileName())
                .fileUrl(uploadResponse.getFileUrl())
                .contentType(uploadResponse.getContentType())
                .fileSize(uploadResponse.getFileSize())
                .isPrimary(Boolean.TRUE.equals(isPrimary))
                .displayOrder(displayOrder)
                .uploadedBy(uploadedBy)
                .build();

        contractFile = contractFileRepository.save(contractFile);
        log.info("Uploaded contract file: {} for contract: {}", contractFile.getId(), contractId);

        return toFileDto(contractFile);
    }

    public List<ContractFileDto> getContractFiles(UUID contractId) {
        List<ContractFile> files = contractFileRepository.findByContractId(contractId);
        return files.stream()
                .map(this::toFileDto)
                .collect(Collectors.toList());
    }

    public Resource viewContractFile(UUID contractId, UUID fileId) {
        ContractFile file = contractFileRepository.findByIdNotDeleted(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        if (!file.getContract().getId().equals(contractId)) {
            throw new IllegalArgumentException("File does not belong to contract: " + contractId);
        }

        return fileStorageService.loadContractFileAsResource(contractId, file.getFileName());
    }

    public Resource downloadContractFile(UUID contractId, UUID fileId) {
        return viewContractFile(contractId, fileId);
    }

    @Transactional
    public void deleteContractFile(UUID contractId, UUID fileId) {
        ContractFile file = contractFileRepository.findByIdNotDeleted(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        if (!file.getContract().getId().equals(contractId)) {
            throw new IllegalArgumentException("File does not belong to contract: " + contractId);
        }

        file.setIsDeleted(true);
        file.setDeletedAt(java.time.OffsetDateTime.now());
        contractFileRepository.save(file);

        try {
            fileStorageService.deleteContractFile(contractId, file.getFileName());
        } catch (Exception e) {
            log.error("Failed to delete physical file: {}", file.getFileName(), e);
        }

        log.info("Deleted contract file: {} for contract: {}", fileId, contractId);
    }

    @Transactional
    public ContractFileDto setPrimaryFile(UUID contractId, UUID fileId) {
        ContractFile file = contractFileRepository.findByIdNotDeleted(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileId));

        if (!file.getContract().getId().equals(contractId)) {
            throw new IllegalArgumentException("File does not belong to contract: " + contractId);
        }

        contractFileRepository.findPrimaryFileByContractId(contractId)
                .ifPresent(primaryFile -> {
                    primaryFile.setIsPrimary(false);
                    contractFileRepository.save(primaryFile);
                });
        file.setIsPrimary(true);
        file = contractFileRepository.save(file);
        log.info("Set primary file: {} for contract: {}", fileId, contractId);

        return toFileDto(file);
    }

    private ContractDto toDto(Contract contract) {
        List<ContractFileDto> files = List.of();
        try {
            if (contract.getFiles() != null) {
                files = contract.getFiles().stream()
                        .filter(f -> f != null && !f.getIsDeleted())
                        .map(file -> {
                            try {
                                return toFileDto(file);
                            } catch (Exception e) {
                                log.warn("[ContractService] Lỗi khi convert file {} sang DTO: {}", 
                                        file != null ? file.getId() : "null", e.getMessage());
                                return null;
                            }
                        })
                        .filter(f -> f != null)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("[ContractService] Lỗi khi load files cho contract {}: {}", 
                    contract.getId(), e.getMessage());
        }

        int reminderCount = calculateReminderCount(contract);
        boolean isFinalReminder = reminderCount == 3;
        boolean needsRenewal = calculateNeedsRenewal(contract);

        return ContractDto.builder()
                .id(contract.getId())
                .unitId(contract.getUnitId())
                .contractNumber(contract.getContractNumber())
                .contractType(contract.getContractType())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .checkoutDate(contract.getCheckoutDate())
                .monthlyRent(contract.getMonthlyRent())
                .totalRent(calculateTotalRent(contract))
                .purchasePrice(contract.getPurchasePrice())
                .paymentMethod(contract.getPaymentMethod())
                .paymentTerms(contract.getPaymentTerms())
                .purchaseDate(contract.getPurchaseDate())
                .notes(contract.getNotes())
                .status(contract.getStatus())
                .createdBy(contract.getCreatedBy())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .updatedBy(contract.getUpdatedBy())
                .renewalReminderSentAt(contract.getRenewalReminderSentAt())
                .renewalDeclinedAt(contract.getRenewalDeclinedAt())
                .renewalStatus(contract.getRenewalStatus())
                .reminderCount(reminderCount > 0 ? reminderCount : null)
                .isFinalReminder(isFinalReminder)
                .needsRenewal(needsRenewal)
                .renewedContractId(contract.getRenewedContractId())
                .files(files)
                .build();
    }

    private ContractDto toDtoSummary(Contract contract) {
        return ContractDto.builder()
                .id(contract.getId())
                .unitId(contract.getUnitId())
                .contractNumber(contract.getContractNumber())
                .contractType(contract.getContractType())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .monthlyRent(contract.getMonthlyRent())
                .totalRent(calculateTotalRent(contract))
                .purchasePrice(contract.getPurchasePrice())
                .paymentMethod(contract.getPaymentMethod())
                .paymentTerms(contract.getPaymentTerms())
                .purchaseDate(contract.getPurchaseDate())
                .notes(contract.getNotes())
                .status(contract.getStatus())
                .createdBy(contract.getCreatedBy())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .updatedBy(contract.getUpdatedBy())
                .renewalReminderSentAt(contract.getRenewalReminderSentAt())
                .renewalDeclinedAt(contract.getRenewalDeclinedAt())
                .renewalStatus(contract.getRenewalStatus())
                .renewedContractId(contract.getRenewedContractId())
                .files(null)
                .build();
    }

    private ContractFileDto toFileDto(ContractFile file) {
        return ContractFileDto.builder()
                .id(file.getId())
                .contractId(file.getContract().getId())
                .fileName(file.getFileName())
                .originalFileName(file.getOriginalFileName())
                .fileUrl(file.getFileUrl())
                .contentType(file.getContentType())
                .fileSize(file.getFileSize())
                .isPrimary(file.getIsPrimary())
                .displayOrder(file.getDisplayOrder())
                .uploadedBy(file.getUploadedBy())
                .uploadedAt(file.getUploadedAt())
                .build();
    }

    @Transactional
    public ContractDto checkoutContract(UUID contractId, LocalDate checkoutDate, UUID updatedBy) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

        if (contract.getEndDate() != null && checkoutDate.isAfter(contract.getEndDate())) {
            throw new IllegalArgumentException("Checkout date must be less than or equal to end date");
        }

        if (checkoutDate.isBefore(contract.getStartDate())) {
            throw new IllegalArgumentException("Checkout date must be after or equal to start date");
        }

        contract.setCheckoutDate(checkoutDate);
        contract.setStatus("CANCELLED");
        contract.setUpdatedBy(updatedBy);
        
        contract = contractRepository.save(contract);
        log.info("Checked out contract: {} with checkout date: {}", contractId, checkoutDate);

        return toDto(contract);
    }

    @Transactional
    public int activateInactiveContracts() {
        LocalDate today = LocalDate.now();
        List<Contract> inactiveContracts = contractRepository.findInactiveContractsByStartDate(today);
        
        int activatedCount = 0;
        for (Contract contract : inactiveContracts) {
            contract.setStatus("ACTIVE");
            contractRepository.save(contract);
            activatedCount++;
            log.info("Activated contract: {} (contract number: {})", contract.getId(), contract.getContractNumber());
        }
        
        if (activatedCount > 0) {
            log.info("Activated {} inactive contract(s) with start date = {}", activatedCount, today);
        }
        
        return activatedCount;
    }

    @Transactional
    public int markExpiredContracts() {
        LocalDate today = LocalDate.now();
        List<Contract> expiredContracts = contractRepository.findContractsNeedingExpired(today);
        
        int expiredCount = 0;
        for (Contract contract : expiredContracts) {
            // When contract expires, set status to EXPIRED
            // renewalStatus remains as is (PENDING, REMINDED, or DECLINED)
            contract.setStatus("EXPIRED");
            contractRepository.save(contract);
            expiredCount++;
            log.info("Marked contract as expired: {} (contract number: {}, endDate: {}, renewalStatus: {})", 
                    contract.getId(), contract.getContractNumber(), contract.getEndDate(), contract.getRenewalStatus());
        }
        
        if (expiredCount > 0) {
            log.info("Marked {} contract(s) as expired with endDate < {}", expiredCount, today);
        }
        
        return expiredCount;
    }

    public BigDecimal calculateTotalRent(Contract contract) {
        if (!"RENTAL".equals(contract.getContractType())) {
            return null;
        }
        
        if (contract.getMonthlyRent() == null || contract.getStartDate() == null) {
            return null;
        }
        
        if (contract.getEndDate() == null) {
            return null;
        }
        
        LocalDate startDate = contract.getStartDate();
        LocalDate endDate = contract.getEndDate();
        BigDecimal monthlyRent = contract.getMonthlyRent();
        
        if (startDate.isAfter(endDate)) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalRent = BigDecimal.ZERO;
        
      
        if (startDate.getYear() == endDate.getYear() && startDate.getMonth() == endDate.getMonth()) {
         
            int daysInMonth = startDate.lengthOfMonth();
            long actualDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            BigDecimal dailyRate = monthlyRent.divide(BigDecimal.valueOf(daysInMonth), 10, RoundingMode.HALF_UP);
            totalRent = dailyRate.multiply(BigDecimal.valueOf(actualDays));
        } else {
          
            int daysInFirstMonth = startDate.lengthOfMonth();
            LocalDate endOfFirstMonth = startDate.withDayOfMonth(daysInFirstMonth);
            long daysInFirstPeriod = ChronoUnit.DAYS.between(startDate, endOfFirstMonth) + 1;
            BigDecimal dailyRateFirstMonth = monthlyRent.divide(BigDecimal.valueOf(daysInFirstMonth), 10, RoundingMode.HALF_UP);
            BigDecimal firstMonthRent = dailyRateFirstMonth.multiply(BigDecimal.valueOf(daysInFirstPeriod));
            totalRent = totalRent.add(firstMonthRent);
            
          
            LocalDate firstDayOfSecondMonth = startDate.plusMonths(1).withDayOfMonth(1);
            LocalDate firstDayOfLastMonth = endDate.withDayOfMonth(1);
            
            if (firstDayOfSecondMonth.isBefore(firstDayOfLastMonth)) {
                long middleMonths = ChronoUnit.MONTHS.between(firstDayOfSecondMonth, firstDayOfLastMonth);
                BigDecimal middleMonthsRent = monthlyRent.multiply(BigDecimal.valueOf(middleMonths));
                totalRent = totalRent.add(middleMonthsRent);
            }
            
          
            int daysInLastMonth = endDate.lengthOfMonth();
            LocalDate firstDayOfLastMonthActual = endDate.withDayOfMonth(1);
            long daysInLastPeriod = ChronoUnit.DAYS.between(firstDayOfLastMonthActual, endDate) + 1;
            BigDecimal dailyRateLastMonth = monthlyRent.divide(BigDecimal.valueOf(daysInLastMonth), 10, RoundingMode.HALF_UP);
            BigDecimal lastMonthRent = dailyRateLastMonth.multiply(BigDecimal.valueOf(daysInLastPeriod));
            totalRent = totalRent.add(lastMonthRent);
        }
        
        return totalRent.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<Contract> findContractsNeedingRenewalReminder() {
        LocalDate today = LocalDate.now();
        // Find contracts with endDate in next 8-32 days (for all 3 reminder levels)
        // Lần 1: 30 ngày trước endDate (28-32 buffer)
        // Lần 2: 22 ngày trước endDate (20-24 buffer) - ngày thứ 8 trong tháng
        // Lần 3: 10 ngày trước endDate (8-12 buffer) - ngày 20 trong tháng
        LocalDate maxDate = today.plusDays(32);
        
        return contractRepository.findContractsNeedingRenewalReminderByDateRange(today, maxDate);
    }

    @Transactional(readOnly = true)
    public List<Contract> findContractsWithRenewalDeclined(OffsetDateTime deadlineDate) {
        return contractRepository.findContractsWithRenewalDeclined(deadlineDate);
    }

    @Transactional
    public void sendRenewalReminder(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
        
        if (!"RENTAL".equals(contract.getContractType())) {
            throw new IllegalArgumentException("Only RENTAL contracts can have renewal reminders");
        }
        
        if (!"ACTIVE".equals(contract.getStatus())) {
            throw new IllegalArgumentException("Only ACTIVE contracts can have renewal reminders");
        }
        
        if (contract.getEndDate() == null) {
            throw new IllegalArgumentException("Contract must have end date for renewal reminder");
        }
        
        String currentRenewalStatus = contract.getRenewalStatus();
        if (!"PENDING".equals(currentRenewalStatus) && !"REMINDED".equals(currentRenewalStatus)) {
            throw new IllegalArgumentException("Contract must be in PENDING or REMINDED status to send renewal reminder. Current status: " + currentRenewalStatus);
        }
        
        // Chỉ set renewalReminderSentAt lần đầu tiên (lần 1)
        // Giữ nguyên thời điểm lần 1 để có thể tính toán lần 2 và lần 3
        if (contract.getRenewalReminderSentAt() == null) {
            contract.setRenewalReminderSentAt(OffsetDateTime.now());
        }
        
        contract.setRenewalStatus("REMINDED");
        contractRepository.save(contract);
        
        if (contract.getRenewalReminderSentAt() != null) {
            long daysSinceFirstReminder = ChronoUnit.DAYS.between(
                contract.getRenewalReminderSentAt().toLocalDate(),
                LocalDate.now()
            );
            log.info("Sent renewal reminder for contract: {} (ends on: {}, {} days since first reminder)", 
                    contractId, contract.getEndDate(), daysSinceFirstReminder);
        } else {
            log.info("Sent renewal reminder for contract: {} (ends on: {})", contractId, contract.getEndDate());
        }
    }

    @Transactional(readOnly = true)
    public List<Contract> findContractsNeedingSecondReminder() {
        OffsetDateTime sevenDaysAgo = OffsetDateTime.now().minusDays(7);
        return contractRepository.findContractsNeedingSecondReminder(sevenDaysAgo);
    }

    @Transactional(readOnly = true)
    public List<Contract> findContractsNeedingThirdReminder() {
        OffsetDateTime twentyDaysAgo = OffsetDateTime.now().minusDays(20);
        return contractRepository.findContractsNeedingThirdReminder(twentyDaysAgo);
    }

    @Transactional
    public void markRenewalDeclined(UUID contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
        
        if (!"RENTAL".equals(contract.getContractType())) {
            throw new IllegalArgumentException("Only RENTAL contracts can have renewal declined");
        }
        
        if (!"ACTIVE".equals(contract.getStatus())) {
            throw new IllegalArgumentException("Only ACTIVE contracts can have renewal declined");
        }
        
        String currentRenewalStatus = contract.getRenewalStatus();
        if (!"PENDING".equals(currentRenewalStatus) && !"REMINDED".equals(currentRenewalStatus)) {
            throw new IllegalArgumentException("Contract must be in PENDING or REMINDED status to mark as declined. Current status: " + currentRenewalStatus);
        }
        
        contract.setRenewalDeclinedAt(OffsetDateTime.now());
        contract.setRenewalStatus("DECLINED");
        contractRepository.save(contract);
        
        log.info("Marked contract {} as renewal declined (was: {})", contractId, currentRenewalStatus);
    }
    @Deprecated
    @Transactional
    public ContractDto extendContract(UUID contractId, LocalDate newEndDate, UUID updatedBy) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
        
        if (!"RENTAL".equals(contract.getContractType())) {
            throw new IllegalArgumentException("Only RENTAL contracts can be extended");
        }
        
        if (!"ACTIVE".equals(contract.getStatus())) {
            throw new IllegalArgumentException("Only ACTIVE contracts can be extended");
        }
        
        if (contract.getEndDate() == null) {
            throw new IllegalArgumentException("Contract must have end date to extend");
        }
        
        if (newEndDate.isBefore(contract.getEndDate()) || newEndDate.isEqual(contract.getEndDate())) {
            throw new IllegalArgumentException("New end date must be after current end date");
        }
        
        contract.setEndDate(newEndDate);
        contract.setRenewalStatus("PENDING");
        contract.setRenewalReminderSentAt(null);
        contract.setRenewalDeclinedAt(null);
        contract.setUpdatedBy(updatedBy);
        
        contract = contractRepository.save(contract);
        log.info("Extended contract {} to new end date: {}. Renewal status reset to PENDING for new cycle.", 
                contractId, newEndDate);
        
        return toDto(contract);
    }

    /**
     * Get contracts that need to show popup to resident
     * These are contracts with renewalStatus = REMINDED
     */
    @Transactional(readOnly = true)
    public List<ContractDto> getContractsNeedingPopup(UUID unitId) {
        List<Contract> contracts = contractRepository.findByUnitIdAndStatus(unitId, "ACTIVE");
        return contracts.stream()
                .filter(c -> "RENTAL".equals(c.getContractType()))
                .filter(c -> "REMINDED".equals(c.getRenewalStatus()))
                .filter(c -> c.getRenewalReminderSentAt() != null)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Calculate reminder count based on days until end date
     * Lần 1: 30 ngày trước endDate
     * Lần 2: 22 ngày trước endDate (ngày thứ 8 trong tháng)
     * Lần 3: 10 ngày trước endDate (ngày 20 trong tháng)
     */
    /**
     * Calculate if contract needs renewal (within 1 month before expiration)
     * Returns true only when contract is in the same time window as reminder 1 (28-32 days before endDate)
     * This is when the status should show "cần gia hạn" instead of just "đang hoạt động"
     */
    private boolean calculateNeedsRenewal(Contract contract) {
        if (contract.getEndDate() == null || !"ACTIVE".equals(contract.getStatus())) {
            return false;
        }
        
        // Only RENTAL contracts can need renewal
        if (!"RENTAL".equals(contract.getContractType())) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate endDate = contract.getEndDate();
        long daysUntilEndDate = ChronoUnit.DAYS.between(today, endDate);
        
        // Needs renewal only when in the same window as reminder 1: 28-32 days before endDate
        // This is when reminder 1 is sent (same time point)
        return daysUntilEndDate >= 28 && daysUntilEndDate <= 32;
    }

    /**
     * Calculate reminder count based on:
     * - Lần 1: Trước 30 ngày hết hạn (28-32 ngày trước endDate)
     * - Lần 2: Đúng ngày 8 của tháng endDate (sau khi đã gửi lần 1)
     * - Lần 3: Đúng ngày 20 của tháng endDate (sau khi đã gửi lần 2)
     */
    public int calculateReminderCount(Contract contract) {
        if (contract.getEndDate() == null || contract.getRenewalReminderSentAt() == null) {
            return 0;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate endDate = contract.getEndDate();
        long daysUntilEndDate = ChronoUnit.DAYS.between(today, endDate);
        LocalDate firstReminderDate = contract.getRenewalReminderSentAt().toLocalDate();
        long daysSinceFirstReminder = ChronoUnit.DAYS.between(firstReminderDate, today);
        
        // Kiểm tra xem có đang trong tháng endDate không
        boolean isInEndDateMonth = today.getYear() == endDate.getYear()
                && today.getMonth() == endDate.getMonth();
        
        // Kiểm tra xem có đang trong tháng trước endDate không (tháng endDate - 1)
        boolean isInMonthBeforeEndDate = false;
        if (endDate.getMonthValue() == 1) {
            // Nếu endDate là tháng 1, tháng trước là tháng 12 của năm trước
            isInMonthBeforeEndDate = today.getYear() == endDate.getYear() - 1
                    && today.getMonthValue() == 12;
        } else {
            // Tháng trước endDate
            isInMonthBeforeEndDate = today.getYear() == endDate.getYear()
                    && today.getMonthValue() == endDate.getMonthValue() - 1;
        }
        
        log.debug("Calculating reminder count for contract {}: today={}, endDate={}, daysUntilEndDate={}, isInEndDateMonth={}, isInMonthBeforeEndDate={}, daysSinceFirstReminder={}", 
                contract.getContractNumber(), today, endDate, daysUntilEndDate, isInEndDateMonth, isInMonthBeforeEndDate, daysSinceFirstReminder);
        
        // Nếu đang trong tháng endDate và đã gửi lần 1
        if (isInEndDateMonth && daysUntilEndDate > 0 && daysUntilEndDate < 30) {
            int dayOfMonth = today.getDayOfMonth();
            // Lần 3: Ngày 20 trở đi (FINAL REMINDER)
            if (dayOfMonth >= 20) {
                log.debug("Contract {}: reminderCount = 3 (dayOfMonth={} >= 20, in endDate month)", contract.getContractNumber(), dayOfMonth);
                return 3;
            }
            // Lần 2: Từ ngày 8 đến trước ngày 20
            if (dayOfMonth >= 8) {
                log.debug("Contract {}: reminderCount = 2 (dayOfMonth={} >= 8, in endDate month)", contract.getContractNumber(), dayOfMonth);
                return 2;
            }
            // Lần 1: Trước ngày 8 (nhưng đã gửi lần 1 trước đó)
            log.debug("Contract {}: reminderCount = 1 (dayOfMonth={} < 8, but first reminder already sent)", contract.getContractNumber(), dayOfMonth);
            return 1;
        }
        
        // Nếu đang trong tháng trước endDate (ví dụ: endDate tháng 1, hôm nay tháng 12)
        // Và đã gửi reminder lần 1, có thể đã gửi reminder lần 2 hoặc 3
        if (isInMonthBeforeEndDate && daysUntilEndDate > 0 && daysUntilEndDate < 60) {
            int dayOfMonth = today.getDayOfMonth();
            // Nếu đã qua ngày 20 của tháng trước endDate, có thể đã gửi reminder lần 3
            // (vì reminder lần 3 được gửi vào ngày 20 của tháng endDate, nhưng nếu endDate là tháng 1 thì ngày 20 tháng 12 cũng có thể trigger)
            if (dayOfMonth >= 20 && daysSinceFirstReminder >= 15) {
                log.debug("Contract {}: reminderCount = 3 (dayOfMonth={} >= 20 in month before endDate, daysSinceFirstReminder={})", 
                        contract.getContractNumber(), dayOfMonth, daysSinceFirstReminder);
                return 3;
            }
            // Nếu đã qua ngày 8 của tháng trước endDate, có thể đã gửi reminder lần 2
            if (dayOfMonth >= 8 && daysSinceFirstReminder >= 7) {
                log.debug("Contract {}: reminderCount = 2 (dayOfMonth={} >= 8 in month before endDate, daysSinceFirstReminder={})", 
                        contract.getContractNumber(), dayOfMonth, daysSinceFirstReminder);
                return 2;
            }
        }
        
        // Nếu không trong tháng endDate, nhưng đã gửi lần 1
        // Lần 1: Trước 30 ngày hết hạn (28-32 ngày trước endDate)
        if (daysUntilEndDate >= 28 && daysUntilEndDate <= 32) {
            log.debug("Contract {}: reminderCount = 1 (daysUntilEndDate={} in range 28-32)", contract.getContractNumber(), daysUntilEndDate);
            return 1;
        }
        
        // Nếu đã gửi lần 1 nhưng không trong khoảng 28-32 ngày và không trong tháng endDate
        // Tính dựa trên số ngày từ lần reminder đầu và vị trí trong chu kỳ reminder
        if (daysUntilEndDate > 0 && daysUntilEndDate < 60) {
            // Check nếu contract được update gần đây (trong vòng 10 phút) - có thể đã force trigger reminder lần 3
            OffsetDateTime updatedAt = contract.getUpdatedAt();
            if (updatedAt != null) {
                long minutesSinceUpdate = ChronoUnit.MINUTES.between(updatedAt, OffsetDateTime.now());
                // Nếu contract được update trong vòng 10 phút và:
                // - Đang trong tháng endDate HOẶC tháng trước endDate
                // - Và đã qua ngày 8 của tháng hiện tại
                // Có thể đã force trigger reminder lần 3
                boolean isInEndDateOrBeforeMonth = isInEndDateMonth || isInMonthBeforeEndDate;
                if (minutesSinceUpdate <= 10 && isInEndDateOrBeforeMonth && today.getDayOfMonth() >= 8) {
                    log.debug("Contract {}: reminderCount = 3 (recently updated {} minutes ago, in endDate month or before, dayOfMonth={})", 
                            contract.getContractNumber(), minutesSinceUpdate, today.getDayOfMonth());
                    return 3;
                }
            }
            
            // Nếu đang trong tháng trước endDate (ví dụ: endDate tháng 1, hôm nay tháng 12)
            // Và đã qua ngày 20 của tháng hiện tại, có thể đã gửi reminder lần 3
            if (isInMonthBeforeEndDate && today.getDayOfMonth() >= 20 && daysSinceFirstReminder >= 15) {
                log.debug("Contract {}: reminderCount = 3 (in month before endDate, dayOfMonth={} >= 20, daysSinceFirstReminder={})", 
                        contract.getContractNumber(), today.getDayOfMonth(), daysSinceFirstReminder);
                return 3;
            }
            // Nếu đang trong tháng trước endDate và đã qua ngày 8, có thể đã gửi reminder lần 2
            if (isInMonthBeforeEndDate && today.getDayOfMonth() >= 8 && daysSinceFirstReminder >= 7) {
                log.debug("Contract {}: reminderCount = 2 (in month before endDate, dayOfMonth={} >= 8, daysSinceFirstReminder={})", 
                        contract.getContractNumber(), today.getDayOfMonth(), daysSinceFirstReminder);
                return 2;
            }
            // Nếu đã qua 20 ngày từ lần reminder đầu, có thể đã gửi reminder lần 3
            if (daysSinceFirstReminder >= 20) {
                log.debug("Contract {}: reminderCount = 3 (daysSinceFirstReminder={} >= 20)", 
                        contract.getContractNumber(), daysSinceFirstReminder);
                return 3;
            }
            // Nếu đã qua 7 ngày từ lần reminder đầu, có thể đã gửi reminder lần 2
            if (daysSinceFirstReminder >= 7) {
                log.debug("Contract {}: reminderCount = 2 (daysSinceFirstReminder={} >= 7)", 
                        contract.getContractNumber(), daysSinceFirstReminder);
                return 2;
            }
            // Vẫn tính là lần 1
            log.debug("Contract {}: reminderCount = 1 (daysUntilEndDate={}, daysSinceFirstReminder={})", 
                    contract.getContractNumber(), daysUntilEndDate, daysSinceFirstReminder);
            return 1;
        }
        
        log.debug("Contract {}: reminderCount = 0 (daysUntilEndDate={})", contract.getContractNumber(), daysUntilEndDate);
        return 0;
    }

    /**
     * Cancel contract (set status to CANCELLED and renewalStatus to DECLINED)
     * If scheduledDate is provided, creates an asset inspection
     */
    @Transactional
    public ContractDto cancelContract(UUID contractId, UUID updatedBy, java.time.LocalDate scheduledDate) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
        
        if (!"RENTAL".equals(contract.getContractType())) {
            throw new IllegalArgumentException("Only RENTAL contracts can be cancelled");
        }
        
        if (!"ACTIVE".equals(contract.getStatus())) {
            throw new IllegalArgumentException("Only ACTIVE contracts can be cancelled");
        }
        
        contract.setStatus("CANCELLED");
        // Set renewalStatus to DECLINED when user cancels the contract
        contract.setRenewalStatus("DECLINED");
        contract.setRenewalDeclinedAt(OffsetDateTime.now());
        contract.setUpdatedBy(updatedBy);
        contract = contractRepository.save(contract);
        
        // Flush to ensure the status change is committed to database before calling base-service
        // This ensures base-service can see the contract as CANCELLED when it queries
        entityManager.flush();
        
        log.info("Cancelled contract: {} (renewalStatus set to DECLINED)", contractId);
        
        // Always create asset inspection when contract is cancelled
        // Use the selected date (scheduledDate) as inspectionDate instead of scheduledDate
        // If scheduledDate is null, use today as inspectionDate
        java.time.LocalDate inspectionDate = scheduledDate != null ? scheduledDate : java.time.LocalDate.now();
        // The selected date is now stored in inspectionDate, not scheduledDate
        // Pass null for scheduledDate since we're using inspectionDate instead
        baseServiceClient.createAssetInspection(contractId, contract.getUnitId(), inspectionDate, null);
        
        return toDto(contract);
    }
    
    /**
     * Cancel contract without scheduled date (backward compatibility)
     */
    @Transactional
    public ContractDto cancelContract(UUID contractId, UUID updatedBy) {
        return cancelContract(contractId, updatedBy, null);
    }

    /**
     * Renew contract - create new contract based on old contract
     * This will be called from the controller which will handle VNPay payment
     */
    @Transactional
    public ContractDto renewContract(UUID oldContractId, LocalDate newStartDate, LocalDate newEndDate, UUID createdBy) {
        Contract oldContract = contractRepository.findById(oldContractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + oldContractId));
        
        if (!"RENTAL".equals(oldContract.getContractType())) {
            throw new IllegalArgumentException("Only RENTAL contracts can be renewed");
        }
        
        if (!"ACTIVE".equals(oldContract.getStatus()) && !"REMINDED".equals(oldContract.getRenewalStatus())) {
            throw new IllegalArgumentException("Contract must be ACTIVE and in REMINDED status to renew");
        }
        
        // Check if contract has already been renewed
        if (oldContract.getRenewedContractId() != null) {
            throw new IllegalArgumentException("Hợp đồng này đã được gia hạn thành công. Không thể gia hạn lại.");
        }
        
        // Validate dates: Ngày kết thúc phải sau ngày bắt đầu và không được trùng nhau
        if (newStartDate.isAfter(newEndDate) || newStartDate.isEqual(newEndDate)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu và không được trùng nhau");
        }
        
        // Validate: Gia hạn phải ít nhất 3 tháng
        // Tính số tháng từ đầu tháng bắt đầu đến đầu tháng kết thúc
        long monthsBetween = java.time.temporal.ChronoUnit.MONTHS.between(
            newStartDate.withDayOfMonth(1), 
            newEndDate.withDayOfMonth(1)
        );
        if (monthsBetween < 3) {
            throw new IllegalArgumentException("Gia hạn hợp đồng phải ít nhất 3 tháng. Ngày kết thúc phải cách ngày bắt đầu ít nhất 3 tháng.");
        }
        
        // Check for overlapping contracts (không được trùng thời gian)
        List<Contract> existingContracts = contractRepository.findByUnitId(oldContract.getUnitId());
        String oldContractNumber = oldContract.getContractNumber();
        
        log.debug("Checking overlap for contract renewal. Old contract: {}, Old contract number: {}", 
                oldContractId, oldContractNumber);
        
        for (Contract existing : existingContracts) {
            // Skip the old contract itself and cancelled/expired contracts
            if (existing.getId().equals(oldContractId) || 
                "CANCELLED".equals(existing.getStatus()) || 
                "EXPIRED".equals(existing.getStatus())) {
                log.debug("Skipping contract {} - same ID or cancelled/expired", existing.getId());
                continue;
            }
            
            String existingContractNumber = existing.getContractNumber();
            
            // Skip renewal contracts (RENEW) of the same original contract
            // These are contracts that were created from renewing this same contract
            // Format: {oldContractNumber}-RENEW-{timestamp}
            if (existingContractNumber != null && 
                existingContractNumber.startsWith(oldContractNumber + "-RENEW-")) {
                log.debug("Skipping RENEW contract {} - same original contract", existingContractNumber);
                continue;
            }
            
            // Also skip if this existing contract is the one that the old contract was renewed into
            // (i.e., oldContract.getRenewedContractId() == existing.getId())
            if (oldContract.getRenewedContractId() != null && 
                oldContract.getRenewedContractId().equals(existing.getId())) {
                log.debug("Skipping contract {} - this is the renewed contract", existing.getId());
                continue;
            }
            
            // Also skip if existing contract is a RENEW contract that was created from the same original contract
            // Check by extracting the original contract number from RENEW contract number
            if (existingContractNumber != null && existingContractNumber.contains("-RENEW-")) {
                String originalContractNumber = existingContractNumber.substring(0, existingContractNumber.indexOf("-RENEW-"));
                if (originalContractNumber.equals(oldContractNumber)) {
                    log.debug("Skipping RENEW contract {} - same original contract number {}", 
                            existingContractNumber, originalContractNumber);
                    continue;
                }
            }
            
            // Skip INACTIVE and PENDING contracts - these are renewal contracts that haven't been paid yet
            // Only check overlap with ACTIVE contracts
            if ("INACTIVE".equals(existing.getStatus()) || "PENDING".equals(existing.getStatus())) {
                log.debug("Skipping contract {} - status is {} (not yet active/paid)", 
                        existingContractNumber, existing.getStatus());
                continue;
            }
            
            // Check if dates overlap (only for ACTIVE contracts)
            // Only check overlap if existing contract's end date is in the future (still active)
            // If existing contract has already ended, allow new contract to start
            if (existing.getStartDate() != null && existing.getEndDate() != null) {
                LocalDate today = LocalDate.now();
                
                // Skip if existing contract has already ended (endDate is in the past)
                // This allows new contracts to start after old contracts have expired
                if (existing.getEndDate().isBefore(today)) {
                    log.debug("Skipping contract {} - end date {} is in the past", 
                            existingContractNumber, existing.getEndDate());
                    continue;
                }
                
                // Check if new contract starts before existing contract ends
                // Only consider it an overlap if new start date is before existing end date
                // If new start date equals existing end date, it's considered consecutive (no overlap)
                boolean overlaps = newStartDate.isBefore(existing.getEndDate()) && 
                                 newEndDate.isAfter(existing.getStartDate());
                
                if (overlaps) {
                    log.warn("Overlap detected: Existing contract {} ({}) overlaps with new renewal period {} to {}", 
                            existingContractNumber, existing.getId(), newStartDate, newEndDate);
                    throw new IllegalArgumentException(
                        String.format("Hợp đồng mới trùng thời gian với hợp đồng hiện có (Số hợp đồng: %s, từ %s đến %s). " +
                                    "Vui lòng chọn khoảng thời gian khác.",
                        existing.getContractNumber(),
                        existing.getStartDate(),
                        existing.getEndDate())
                    );
                }
            }
        }
        
        // Check if start date is today - if not, status should be INACTIVE
        LocalDate today = LocalDate.now();
        String newStatus = newStartDate.equals(today) ? "ACTIVE" : "INACTIVE";
        
        // Create new contract based on old contract
        Contract newContract = Contract.builder()
                .unitId(oldContract.getUnitId())
                .contractNumber(oldContract.getContractNumber()) // Same contract number
                .contractType(oldContract.getContractType())
                .startDate(newStartDate)
                .endDate(newEndDate)
                .monthlyRent(oldContract.getMonthlyRent())
                .notes(oldContract.getNotes())
                .status(newStatus)
                .renewalStatus("PENDING")
                .createdBy(createdBy)
                .build();
        
        newContract = contractRepository.save(newContract);
        log.info("Created renewal contract: {} for old contract: {}", newContract.getId(), oldContractId);
        
        return toDto(newContract);
    }

    /**
     * Create VNPay payment URL for contract renewal
     */
    @Transactional
    public ContractRenewalResponse createRenewalPaymentUrl(UUID contractId, 
                                                           LocalDate newStartDate, 
                                                           LocalDate newEndDate,
                                                           UUID createdBy,
                                                           String clientIp) {
        try {
            Contract oldContract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
            
            if (!"RENTAL".equals(oldContract.getContractType())) {
                throw new IllegalArgumentException("Only RENTAL contracts can be renewed");
            }
            
            if (oldContract.getMonthlyRent() == null) {
                throw new IllegalArgumentException("Contract monthly rent is required for renewal");
            }
            
            // Check if contract has already been renewed
            if (oldContract.getRenewedContractId() != null) {
                throw new IllegalArgumentException("Hợp đồng này đã được gia hạn thành công. Không thể gia hạn lại.");
            }
            
            // Validate dates: Ngày kết thúc phải sau ngày bắt đầu và không được trùng nhau
            if (newStartDate.isAfter(newEndDate) || newStartDate.isEqual(newEndDate)) {
                throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu và không được trùng nhau");
            }
            
            // Validate: Gia hạn phải ít nhất 3 tháng
            long monthsBetween = java.time.temporal.ChronoUnit.MONTHS.between(
                newStartDate.withDayOfMonth(1), 
                newEndDate.withDayOfMonth(1)
            );
            if (monthsBetween < 3) {
                throw new IllegalArgumentException("Gia hạn hợp đồng phải ít nhất 3 tháng. Ngày kết thúc phải cách ngày bắt đầu ít nhất 3 tháng.");
            }
            
            // Check for overlapping contracts (không được trùng thời gian)
            List<Contract> existingContracts = contractRepository.findByUnitId(oldContract.getUnitId());
            String oldContractNumber = oldContract.getContractNumber();
            
            log.info("🔍 Checking overlap for contract renewal. Old contract ID: {}, Old contract number: {}", 
                    contractId, oldContractNumber);
            log.info("🔍 Total existing contracts found: {}", existingContracts.size());
            
            for (Contract existing : existingContracts) {
                String existingContractNumber = existing.getContractNumber();
                log.info("🔍 Checking contract: {} ({}), status: {}", 
                        existingContractNumber, existing.getId(), existing.getStatus());
                
                // Skip the old contract itself and cancelled/expired contracts
                if (existing.getId().equals(contractId) || 
                    "CANCELLED".equals(existing.getStatus()) || 
                    "EXPIRED".equals(existing.getStatus())) {
                    log.info("✅ Skipping contract {} - same ID or cancelled/expired", existing.getId());
                    continue;
                }
                
                // Skip renewal contracts (RENEW) of the same original contract
                // These are contracts that were created from renewing this same contract
                // Format: {oldContractNumber}-RENEW-{timestamp}
                if (existingContractNumber != null) {
                    String checkPrefix = oldContractNumber + "-RENEW-";
                    log.info("🔍 Checking if {} starts with {}", existingContractNumber, checkPrefix);
                    if (existingContractNumber.startsWith(checkPrefix)) {
                        log.info("✅ Skipping RENEW contract {} - same original contract (startsWith check)", existingContractNumber);
                        continue;
                    }
                    
                    // Also skip if existing contract is a RENEW contract that was created from the same original contract
                    // Check by extracting the original contract number from RENEW contract number
                    if (existingContractNumber.contains("-RENEW-")) {
                        String originalContractNumber = existingContractNumber.substring(0, existingContractNumber.indexOf("-RENEW-"));
                        log.info("🔍 Extracted original contract number from RENEW: {} (from {}), comparing with {}", 
                                originalContractNumber, existingContractNumber, oldContractNumber);
                        if (originalContractNumber.equals(oldContractNumber)) {
                            log.info("✅ Skipping RENEW contract {} - same original contract number {}", 
                                    existingContractNumber, originalContractNumber);
                            continue;
                        } else {
                            log.info("⚠️ RENEW contract {} has different original contract number: {} vs {}", 
                                    existingContractNumber, originalContractNumber, oldContractNumber);
                        }
                    }
                }
                
                // Also skip if this existing contract is the one that the old contract was renewed into
                // (i.e., oldContract.getRenewedContractId() == existing.getId())
                if (oldContract.getRenewedContractId() != null && 
                    oldContract.getRenewedContractId().equals(existing.getId())) {
                    log.info("✅ Skipping contract {} - this is the renewed contract", existing.getId());
                    continue;
                }
                
                // Skip INACTIVE and PENDING contracts - these are renewal contracts that haven't been paid yet
                // Only check overlap with ACTIVE contracts
                if ("INACTIVE".equals(existing.getStatus()) || "PENDING".equals(existing.getStatus())) {
                    log.info("✅ Skipping contract {} - status is {} (not yet active/paid)", 
                            existingContractNumber, existing.getStatus());
                    continue;
                }
                
                // Check if dates overlap (only for ACTIVE contracts)
                // Only check overlap if existing contract's end date is in the future (still active)
                // If existing contract has already ended, allow new contract to start
                if (existing.getStartDate() != null && existing.getEndDate() != null) {
                    LocalDate today = LocalDate.now();
                    
                    // Skip if existing contract has already ended (endDate is in the past)
                    // This allows new contracts to start after old contracts have expired
                    if (existing.getEndDate().isBefore(today)) {
                        log.info("✅ Skipping contract {} - end date {} is in the past", 
                                existingContractNumber, existing.getEndDate());
                        continue;
                    }
                    
                    // Check if new contract starts before existing contract ends
                    // Only consider it an overlap if new start date is before existing end date
                    // If new start date equals existing end date, it's considered consecutive (no overlap)
                    boolean overlaps = newStartDate.isBefore(existing.getEndDate()) && 
                                     newEndDate.isAfter(existing.getStartDate());
                    
                    if (overlaps) {
                        log.warn("❌ Overlap detected: Existing contract {} ({}) overlaps with new renewal period {} to {}", 
                                existingContractNumber, existing.getId(), newStartDate, newEndDate);
                        throw new IllegalArgumentException(
                            String.format("Hợp đồng mới trùng thời gian với hợp đồng hiện có (Số hợp đồng: %s, từ %s đến %s). " +
                                        "Vui lòng chọn khoảng thời gian khác.",
                            existing.getContractNumber(),
                            existing.getStartDate(),
                            existing.getEndDate())
                        );
                    }
                }
            }
            
            // Create new contract first (with PENDING status, will be activated after payment)
            // Generate new contract number for renewal (append timestamp to avoid duplicate)
            String newContractNumber = oldContract.getContractNumber() + "-RENEW-" + System.currentTimeMillis();
            
            // Check if contract number already exists (very unlikely but safe)
            int retryCount = 0;
            while (contractRepository.findByContractNumber(newContractNumber).isPresent() && retryCount < 5) {
                newContractNumber = oldContract.getContractNumber() + "-RENEW-" + System.currentTimeMillis() + "-" + retryCount;
                retryCount++;
            }
            
            Contract newContract = Contract.builder()
                    .unitId(oldContract.getUnitId())
                    .contractNumber(newContractNumber)
                    .contractType(oldContract.getContractType())
                    .startDate(newStartDate)
                    .endDate(newEndDate)
                    .monthlyRent(oldContract.getMonthlyRent())
                    .notes(oldContract.getNotes())
                    .status("INACTIVE") // Will be activated after payment
                    .renewalStatus("PENDING")
                    .createdBy(createdBy)
                    .build();
            
            newContract = contractRepository.save(newContract);
            
            // Calculate total amount
            BigDecimal totalAmount = calculateTotalRent(newContract);
            if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("Invalid contract amount for payment: contractId={}, totalAmount={}", 
                        newContract.getId(), totalAmount);
                throw new IllegalArgumentException("Invalid contract amount for payment: " + totalAmount);
            }
            
            // Create VNPay payment URL
            // Use newContractId as part of orderId to track it
            Long orderId = newContract.getId().getMostSignificantBits() & Long.MAX_VALUE;
            String orderInfo = String.format("Gia hạn hợp đồng %s - ContractId:%s", 
                    oldContract.getContractNumber(), newContract.getId());
            
            String returnUrlBase = vnpayProperties.getContractRenewalReturnUrl();
            if (returnUrlBase == null || returnUrlBase.isEmpty()) {
                log.error("Contract renewal return URL is not configured");
                throw new IllegalStateException("Contract renewal return URL is not configured. Please check vnpay.contract-renewal-return-url or vnpay.base-url in application properties");
            }
            
            String returnUrl = returnUrlBase + "?contractId=" + newContract.getId();
            
            VnpayService.VnpayPaymentResult paymentResult = vnpayService.createPaymentUrl(
                    orderId,
                    orderInfo,
                    totalAmount,
                    clientIp,
                    returnUrl
            );
            
            log.info("Created VNPay payment URL for contract renewal: contractId={}, newContractId={}, amount={}", 
                    contractId, newContract.getId(), totalAmount);
            
            return ContractRenewalResponse.builder()
                    .newContractId(newContract.getId())
                    .contractNumber(newContract.getContractNumber())
                    .totalAmount(totalAmount)
                    .paymentUrl(paymentResult.paymentUrl())
                    .message("Vui lòng thanh toán để hoàn tất gia hạn hợp đồng")
                    .build();
        } catch (IllegalArgumentException e) {
            log.error("Error creating renewal payment URL: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating renewal payment URL for contractId={}", contractId, e);
            throw new RuntimeException("Failed to create payment URL: " + e.getMessage(), e);
        }
    }

    /**
     * Handle VNPay callback for contract renewal payment
     * Note: This requires storing txnRef -> contractId mapping when creating payment URL
     * For now, we'll extract contractId from orderInfo or use a query parameter
     */
    @Transactional
    public ContractDto handleVnpayCallback(Map<String, String> params, UUID contractId) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("Missing callback data from VNPAY");
        }
        
        boolean signatureValid = vnpayService.validateReturn(params);
        if (!signatureValid) {
            throw new IllegalArgumentException("Invalid VNPAY signature");
        }
        
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        
        if (txnRef == null || txnRef.isEmpty()) {
            throw new IllegalArgumentException("Missing transaction reference from VNPAY");
        }
        
        log.info("Processing VNPay callback: txnRef={}, responseCode={}, contractId={}", txnRef, responseCode, contractId);
        
        if ("00".equals(responseCode)) {
            // Payment successful - complete the renewal
            // Get contract to retrieve unitId
            Contract newContract = contractRepository.findById(contractId)
                    .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
            
            // Get residentId from contract's unitId
            Optional<UUID> residentIdOpt = baseServiceClient.getPrimaryResidentIdByUnitId(newContract.getUnitId());
            if (residentIdOpt.isEmpty()) {
                log.warn("⚠️ Cannot find resident for unitId: {}. This may happen if unit has no active household.", newContract.getUnitId());
                log.warn("⚠️ Contract createdBy (userId): {}", newContract.getCreatedBy());
                throw new IllegalArgumentException("Cannot find resident for contract unit: " + newContract.getUnitId() + 
                        ". Please ensure the unit has an active household with a primary resident.");
            }
            
            return completeRenewalPayment(contractId, residentIdOpt.get(), txnRef);
        } else {
            throw new IllegalArgumentException("VNPay payment failed with response code: " + responseCode);
        }
    }

    /**
     * Extract VNPay params from HttpServletRequest
     */
    public Map<String, String> extractVnpayParams(jakarta.servlet.http.HttpServletRequest request) {
        return vnpayService.extractParams(request);
    }

    /**
     * Complete contract renewal after successful payment
     */
    @Transactional
    public ContractDto completeRenewalPayment(UUID newContractId, UUID residentId, String vnpayTransactionRef) {
        Contract newContract = contractRepository.findById(newContractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + newContractId));
        
        if (!"PENDING".equals(newContract.getRenewalStatus())) {
            throw new IllegalArgumentException("Contract is not in PENDING renewal status");
        }
        
        // Get unit code
        Optional<String> unitCodeOpt = baseServiceClient.getUnitCodeByUnitId(newContract.getUnitId());
        String unitCode = unitCodeOpt.orElse("N/A");
        
        // Calculate total amount
        BigDecimal totalAmount = calculateTotalRent(newContract);
        
        // Create invoice
        UUID invoiceId = invoiceClient.createContractRenewalInvoice(
                newContract.getId(),
                newContract.getUnitId(),
                residentId,
                newContract.getContractNumber(),
                unitCode,
                totalAmount,
                newContract.getStartDate(),
                newContract.getEndDate()
        );
        
        if (invoiceId == null) {
            log.warn("Failed to create invoice for contract renewal, but continuing...");
        }
        
        // Update contract status
        LocalDate today = LocalDate.now();
        if (newContract.getStartDate().equals(today)) {
            newContract.setStatus("ACTIVE");
        } else {
            newContract.setStatus("INACTIVE"); // Will be activated by scheduler when start date arrives
        }
        
        newContract.setRenewalStatus("PENDING"); // Reset for new cycle
        newContract = contractRepository.save(newContract);
        
        // Find and update the old contract to mark it as renewed
        // The new contract number format is: {oldContractNumber}-RENEW-{timestamp}
        String newContractNumber = newContract.getContractNumber();
        if (newContractNumber != null && newContractNumber.contains("-RENEW-")) {
            String oldContractNumber = newContractNumber.substring(0, newContractNumber.indexOf("-RENEW-"));
            Optional<Contract> oldContractOpt = contractRepository.findByContractNumber(oldContractNumber);
            if (oldContractOpt.isPresent()) {
                Contract oldContract = oldContractOpt.get();
                oldContract.setRenewedContractId(newContract.getId());
                contractRepository.save(oldContract);
                log.info("Marked old contract {} as renewed with new contract {}", 
                        oldContract.getId(), newContract.getId());
            } else {
                log.warn("Could not find old contract with number: {}", oldContractNumber);
            }
        }
        
        log.info("Completed contract renewal payment: contractId={}, invoiceId={}, vnpayTxnRef={}", 
                newContract.getId(), invoiceId, vnpayTransactionRef);
        
        return toDto(newContract);
    }
    public void triggerRenewalReminders() {
        log.info("Manual trigger: Send renewal reminders");
        java.time.LocalDate today = java.time.LocalDate.now();
        
        // Get all active RENTAL contracts that need reminders
        List<com.QhomeBase.datadocsservice.model.Contract> allContracts = findContractsNeedingRenewalReminder();
        log.info("Found {} contract(s) that may need renewal reminders", allContracts.size());
        
        int firstReminderCount = 0;
        int secondReminderCount = 0;
        int thirdReminderCount = 0;
        
        for (com.QhomeBase.datadocsservice.model.Contract contract : allContracts) {
            if (contract.getEndDate() == null || !"RENTAL".equals(contract.getContractType()) 
                    || !"ACTIVE".equals(contract.getStatus())) {
                continue;
            }
            
            java.time.LocalDate endDate = contract.getEndDate();
            
            // Calculate days until end date
            long daysUntilEndDate = java.time.temporal.ChronoUnit.DAYS.between(today, endDate);
            
            log.debug("Checking contract {}: endDate={}, today={}, daysUntilEndDate={}, renewalStatus={}, reminderSentAt={}", 
                    contract.getContractNumber(), endDate, today, daysUntilEndDate,
                    contract.getRenewalStatus(), contract.getRenewalReminderSentAt());
            
            try {
                // Lần 1: Trước 30 ngày hết hạn hợp đồng
                // Gửi khi còn 28-32 ngày (buffer để đảm bảo không bỏ sót)
                if (daysUntilEndDate >= 28 && daysUntilEndDate <= 32 
                        && contract.getRenewalReminderSentAt() == null) {
                    sendRenewalReminder(contract.getId());
                    firstReminderCount++;
                    log.info("✅ Sent FIRST renewal reminder for contract {} (expires on {}, {} days until end date)", 
                            contract.getContractNumber(), endDate, daysUntilEndDate);
                }
                // Lần 2: Đúng ngày 8 của tháng endDate
                // Chỉ gửi nếu:
                // - Đã gửi lần 1 (renewalReminderSentAt != null)
                // - Hôm nay là ngày 8 của tháng endDate
                // - Contract vẫn trong tháng cuối (daysUntilEndDate > 0 và < 30)
                // - Lần 1 đã được gửi trước hôm nay (đảm bảo không gửi lần 2 trước lần 1)
                else if (contract.getRenewalReminderSentAt() != null
                        && today.getYear() == endDate.getYear()
                        && today.getMonth() == endDate.getMonth()
                        && today.getDayOfMonth() == 8
                        && daysUntilEndDate > 0 && daysUntilEndDate < 30) {
                    // Check if we already sent reminder 2 (by checking if reminder was sent before today)
                    java.time.LocalDate firstReminderDate = contract.getRenewalReminderSentAt().toLocalDate();
                    // Lần 2 chỉ gửi 1 lần vào ngày 8, và chỉ gửi nếu lần 1 đã được gửi trước đó
                    // Kiểm tra: lần 1 phải được gửi trước ngày 8 (không phải cùng ngày 8)
                    if (firstReminderDate.isBefore(today) && firstReminderDate.getDayOfMonth() != 8) {
                        sendRenewalReminder(contract.getId());
                        secondReminderCount++;
                        log.info("✅ Sent SECOND renewal reminder for contract {} (expires on {}, today is day 8 of endDate month)", 
                                contract.getContractNumber(), endDate);
                    } else {
                        log.debug("⏭️ Skipping reminder 2 for contract {}: firstReminderDate={}, today={}", 
                                contract.getContractNumber(), firstReminderDate, today);
                    }
                }
                // Lần 3: Đúng ngày 20 của tháng endDate - BẮT BUỘC
                // Chỉ gửi nếu:
                // - Đã gửi lần 1 (renewalReminderSentAt != null)
                // - Hôm nay là ngày 20 của tháng endDate
                // - Contract vẫn trong tháng cuối (daysUntilEndDate > 0 và < 30)
                // - Lần 1 đã được gửi trước hôm nay (đảm bảo không gửi lần 3 trước lần 1)
                else if (contract.getRenewalReminderSentAt() != null
                        && today.getYear() == endDate.getYear()
                        && today.getMonth() == endDate.getMonth()
                        && today.getDayOfMonth() == 20
                        && daysUntilEndDate > 0 && daysUntilEndDate < 30) {
                    // Check if we already sent reminder 3 (by checking if reminder was sent before today)
                    java.time.LocalDate firstReminderDate = contract.getRenewalReminderSentAt().toLocalDate();
                    // Lần 3 chỉ gửi 1 lần vào ngày 20, và chỉ gửi nếu lần 1 đã được gửi trước đó
                    // Kiểm tra: lần 1 phải được gửi trước ngày 20 (không phải cùng ngày 20)
                    if (firstReminderDate.isBefore(today) && firstReminderDate.getDayOfMonth() != 20) {
                        sendRenewalReminder(contract.getId());
                        thirdReminderCount++;
                        log.info("✅ Sent THIRD (FINAL) renewal reminder for contract {} (expires on {}, today is day 20 of endDate month - BẮT BUỘC HỦY HOẶC GIA HẠN)", 
                                contract.getContractNumber(), endDate);
                    } else {
                        log.debug("⏭️ Skipping reminder 3 for contract {}: firstReminderDate={}, today={}", 
                                contract.getContractNumber(), firstReminderDate, today);
                    }
                }
            } catch (Exception e) {
                log.error("Error sending renewal reminder for contract {}", contract.getId(), e);
            }
        }
        
        log.info("Manual trigger completed: Sent {} first reminder(s), {} second reminder(s), {} third reminder(s)", 
                firstReminderCount, secondReminderCount, thirdReminderCount);
    }

}

