package com.QhomeBase.datadocsservice.service;

import com.QhomeBase.datadocsservice.dto.*;
import com.QhomeBase.datadocsservice.model.Contract;
import com.QhomeBase.datadocsservice.model.ContractFile;
import com.QhomeBase.datadocsservice.repository.ContractFileRepository;
import com.QhomeBase.datadocsservice.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractFileRepository contractFileRepository;
    private final FileStorageService fileStorageService;

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
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
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
                            log.error("❌ [ContractService] Lỗi khi convert contract {} sang DTO: {}", 
                                    contract.getId(), e.getMessage(), e);
                            // Return a minimal DTO to avoid breaking the entire list
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
                                    .files(List.of()) // Empty files list on error
                                    .build();
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ [ContractService] Lỗi khi lấy contracts cho unit {}: {}", unitId, e.getMessage(), e);
            throw new RuntimeException("Không thể lấy danh sách hợp đồng: " + e.getMessage(), e);
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
                                log.warn("⚠️ [ContractService] Lỗi khi convert file {} sang DTO: {}", 
                                        file != null ? file.getId() : "null", e.getMessage());
                                return null;
                            }
                        })
                        .filter(f -> f != null)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("⚠️ [ContractService] Lỗi khi load files cho contract {}: {}", 
                    contract.getId(), e.getMessage());
            // Continue with empty files list
        }

        return ContractDto.builder()
                .id(contract.getId())
                .unitId(contract.getUnitId())
                .contractNumber(contract.getContractNumber())
                .contractType(contract.getContractType())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .checkoutDate(contract.getCheckoutDate())
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
                .files(files)
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

        // Validate checkout date must be less than end date
        if (contract.getEndDate() != null && checkoutDate.isAfter(contract.getEndDate())) {
            throw new IllegalArgumentException("Checkout date must be less than or equal to end date");
        }

        // Validate checkout date must be after or equal to start date
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
}

