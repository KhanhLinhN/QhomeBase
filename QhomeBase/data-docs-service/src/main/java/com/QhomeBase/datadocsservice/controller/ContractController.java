package com.QhomeBase.datadocsservice.controller;

import com.QhomeBase.datadocsservice.dto.*;
import com.QhomeBase.datadocsservice.service.ContractService;
import com.QhomeBase.datadocsservice.service.PdfFieldMapper;
import com.QhomeBase.datadocsservice.service.PdfFormFillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Contracts", description = "Contract management APIs")
public class ContractController {

    private final ContractService contractService;
    private final PdfFormFillService pdfFormFillService;

    @PostMapping
    @Operation(summary = "Create contract", description = "Create a new contract")
    public ResponseEntity<ContractDto> createContract(
            @Valid @RequestBody CreateContractRequest request,
            @RequestParam(value = "createdBy", required = false) UUID createdBy) {
        
        if (createdBy == null) {
            createdBy = UUID.randomUUID();
        }
        
        ContractDto contract = contractService.createContract(request, createdBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(contract);
    }

    @PutMapping("/{contractId}")
    @Operation(summary = "Update contract", description = "Update an existing contract")
    public ResponseEntity<ContractDto> updateContract(
            @PathVariable UUID contractId,
            @Valid @RequestBody UpdateContractRequest request,
            @RequestParam(value = "updatedBy", required = false) UUID updatedBy) {
        
        if (updatedBy == null) {
            updatedBy = UUID.randomUUID();
        }
        
        ContractDto contract = contractService.updateContract(contractId, request, updatedBy);
        return ResponseEntity.ok(contract);
    }

    @GetMapping("/{contractId}")
    @Operation(summary = "Get contract", description = "Get contract by ID")
    public ResponseEntity<ContractDto> getContract(@PathVariable UUID contractId) {
        ContractDto contract = contractService.getContractById(contractId);
        return ResponseEntity.ok(contract);
    }

    @GetMapping("/unit/{unitId}")
    @Operation(summary = "Get contracts by unit", description = "Get all contracts for a specific unit")
    public ResponseEntity<List<ContractDto>> getContractsByUnit(@PathVariable UUID unitId) {
        List<ContractDto> contracts = contractService.getContractsByUnitId(unitId);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active contracts", description = "Get all active contracts")
    public ResponseEntity<List<ContractDto>> getActiveContracts() {
        List<ContractDto> contracts = contractService.getActiveContracts();
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/unit/{unitId}/active")
    @Operation(summary = "Get active contracts by unit", description = "Get active contracts for a specific unit")
    public ResponseEntity<List<ContractDto>> getActiveContractsByUnit(@PathVariable UUID unitId) {
        List<ContractDto> contracts = contractService.getActiveContractsByUnit(unitId);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all contracts", description = "Get all contracts (optional filter by contractType)")
    public ResponseEntity<List<ContractDto>> getAllContracts(
            @RequestParam(value = "contractType", required = false) String contractType) {
        try {
            log.info("Getting all contracts with contractType filter: {}", contractType);
            List<ContractDto> contracts;
            if (contractType != null && !contractType.isEmpty()) {
                contracts = contractService.getContractsByType(contractType);
            } else {
                contracts = contractService.getAllContracts();
            }
            log.info("Found {} contracts", contracts.size());
            return ResponseEntity.ok(contracts);
        } catch (IllegalArgumentException ex) {
            log.error("Invalid request: {}", ex.getMessage(), ex);
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            log.error("Error getting contracts: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{contractId}")
    @Operation(summary = "Delete contract", description = "Delete a contract")
    public ResponseEntity<Void> deleteContract(@PathVariable UUID contractId) {
        contractService.deleteContract(contractId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{contractId}/checkout")
    @Operation(summary = "Checkout contract", description = "Set checkout date for a contract and change status to CANCELLED. Checkout date must be less than end date.")
    public ResponseEntity<ContractDto> checkoutContract(
            @PathVariable UUID contractId,
            @RequestParam("checkoutDate") java.time.LocalDate checkoutDate,
            @RequestParam(value = "updatedBy", required = false) UUID updatedBy) {
        
        if (updatedBy == null) {
            updatedBy = UUID.randomUUID();
        }
        
        ContractDto contract = contractService.checkoutContract(contractId, checkoutDate, updatedBy);
        return ResponseEntity.ok(contract);
    }

    @PutMapping("/activate-inactive")
    @Operation(summary = "Activate inactive contracts", description = "Activate all contracts with status INACTIVE and startDate = today")
    public ResponseEntity<Map<String, Object>> activateInactiveContracts() {
        int activatedCount = contractService.activateInactiveContracts();
        return ResponseEntity.ok(Map.of(
            "message", "Activated " + activatedCount + " contract(s)",
            "activatedCount", activatedCount
        ));
    }

    @PutMapping("/{contractId}/extend")
    @Operation(summary = "Extend contract", description = "Extend a RENTAL contract by updating the end date. This resets the renewal status to PENDING.")
    public ResponseEntity<ContractDto> extendContract(
            @PathVariable UUID contractId,
            @RequestParam("newEndDate") java.time.LocalDate newEndDate,
            @RequestParam(value = "updatedBy", required = false) UUID updatedBy) {
        ContractDto contract = contractService.extendContract(contractId, newEndDate, updatedBy);
        return ResponseEntity.ok(contract);
    }

    @PostMapping("/renewal/trigger-reminders")
    @Operation(summary = "Trigger renewal reminders manually", description = "Manually trigger the renewal reminder job (for testing). This sends reminders for contracts expiring within 30 days.")
    public ResponseEntity<Map<String, Object>> triggerRenewalReminders() {
        log.info("Manual trigger: Send renewal reminders");
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate thirtyDaysFromToday = today.plusDays(30);
        
        int firstReminderCount = 0;
        int secondReminderCount = 0;
        
        List<com.QhomeBase.datadocsservice.model.Contract> firstReminderContracts = contractService.findContractsNeedingRenewalReminder();
        for (com.QhomeBase.datadocsservice.model.Contract contract : firstReminderContracts) {
            try {
                if (contract.getEndDate() != null 
                        && contract.getRenewalReminderSentAt() == null
                        && !contract.getEndDate().isBefore(today)
                        && !contract.getEndDate().isAfter(thirtyDaysFromToday)) {
                    contractService.sendRenewalReminder(contract.getId());
                    firstReminderCount++;
                    log.info("Sent first renewal reminder for contract {} (expires on {}, within 30 days from today)", 
                            contract.getContractNumber(), contract.getEndDate());
                }
            } catch (Exception e) {
                log.error("Error sending first renewal reminder for contract {}", contract.getId(), e);
            }
        }
        
        List<com.QhomeBase.datadocsservice.model.Contract> secondReminderContracts = contractService.findContractsNeedingSecondReminder();
        for (com.QhomeBase.datadocsservice.model.Contract contract : secondReminderContracts) {
            try {
                if (contract.getEndDate() != null 
                        && "REMINDED".equals(contract.getRenewalStatus())
                        && contract.getRenewalReminderSentAt() != null) {
                    long daysSinceFirstReminder = java.time.temporal.ChronoUnit.DAYS.between(
                        contract.getRenewalReminderSentAt().toLocalDate(),
                        today
                    );
                    
                    if (daysSinceFirstReminder >= 7 && daysSinceFirstReminder < 20) {
                        contractService.sendRenewalReminder(contract.getId());
                        secondReminderCount++;
                        log.info("Sent second renewal reminder for contract {} (expires on {}, {} days since first reminder)", 
                                contract.getContractNumber(), contract.getEndDate(), daysSinceFirstReminder);
                    }
                }
            } catch (Exception e) {
                log.error("Error sending second renewal reminder for contract {}", contract.getId(), e);
            }
        }
        
        int thirdReminderCount = 0;
        List<com.QhomeBase.datadocsservice.model.Contract> thirdReminderContracts = contractService.findContractsNeedingThirdReminder();
        for (com.QhomeBase.datadocsservice.model.Contract contract : thirdReminderContracts) {
            try {
                if (contract.getEndDate() != null 
                        && "REMINDED".equals(contract.getRenewalStatus())
                        && contract.getRenewalReminderSentAt() != null) {
                    long daysSinceFirstReminder = java.time.temporal.ChronoUnit.DAYS.between(
                        contract.getRenewalReminderSentAt().toLocalDate(),
                        today
                    );
                    
                    if (daysSinceFirstReminder >= 20) {
                        contractService.sendRenewalReminder(contract.getId());
                        thirdReminderCount++;
                        log.info("Sent third (FINAL) renewal reminder for contract {} (expires on {}, {} days since first reminder - THIS IS THE DEADLINE)", 
                                contract.getContractNumber(), contract.getEndDate(), daysSinceFirstReminder);
                    }
                }
            } catch (Exception e) {
                log.error("Error sending third renewal reminder for contract {}", contract.getId(), e);
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Renewal reminders triggered manually",
            "firstRemindersSent", firstReminderCount,
            "secondRemindersSent", secondReminderCount,
            "thirdRemindersSent", thirdReminderCount,
            "totalSent", firstReminderCount + secondReminderCount + thirdReminderCount
        ));
    }

    @PostMapping("/renewal/trigger-declined")
    @Operation(summary = "Trigger mark renewal declined manually", description = "Manually trigger the job to mark contracts as renewal declined (for testing). This marks contracts that have passed the 20-day deadline.")
    public ResponseEntity<Map<String, Object>> triggerMarkRenewalDeclined() {
        log.info("Manual trigger: Mark renewal declined");
        java.time.OffsetDateTime deadlineDate = java.time.OffsetDateTime.now().minusDays(20);
        
        List<com.QhomeBase.datadocsservice.model.Contract> contracts = contractService.findContractsWithRenewalDeclined(deadlineDate);
        
        int declinedCount = 0;
        for (com.QhomeBase.datadocsservice.model.Contract contract : contracts) {
            try {
                if ("REMINDED".equals(contract.getRenewalStatus())) {
                    long daysSinceFirstReminder = java.time.temporal.ChronoUnit.DAYS.between(
                        contract.getRenewalReminderSentAt().toLocalDate(),
                        java.time.LocalDate.now()
                    );
                    
                    if (daysSinceFirstReminder >= 20) {
                        contractService.markRenewalDeclined(contract.getId());
                        declinedCount++;
                        log.info("Marked contract {} as renewal declined (reminder sent on {}, {} days ago - deadline passed)", 
                                contract.getContractNumber(), contract.getRenewalReminderSentAt(), daysSinceFirstReminder);
                    }
                }
            } catch (Exception e) {
                log.error("Error marking contract {} as renewal declined", contract.getId(), e);
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Mark renewal declined triggered manually",
            "declinedCount", declinedCount
        ));
    }

    @PostMapping("/status/trigger-expired")
    @Operation(summary = "Trigger mark expired contracts manually", description = "Manually trigger the job to mark expired contracts (for testing). This marks ACTIVE contracts with endDate < today as EXPIRED.")
    public ResponseEntity<Map<String, Object>> triggerMarkExpiredContracts() {
        log.info("Manual trigger: Mark expired contracts");
        int expiredCount = contractService.markExpiredContracts();
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Mark expired contracts triggered manually",
            "contractsMarkedExpired", expiredCount
        ));
    }

    @PutMapping("/{contractId}/renewal/decline")
    @Operation(summary = "Decline contract renewal manually", description = "Manually decline renewal for a contract. This marks the contract's renewal status as DECLINED immediately, even if it's still within the reminder period. Contract remains ACTIVE until endDate.")
    public ResponseEntity<Map<String, Object>> declineRenewal(
            @PathVariable UUID contractId,
            @RequestParam(value = "updatedBy", required = false) UUID updatedBy) {
        try {
            contractService.markRenewalDeclined(contractId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Contract renewal declined successfully",
                "contractId", contractId.toString()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error declining renewal for contract {}", contractId, e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Failed to decline renewal: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{contractId}/files")
    @Operation(summary = "Upload contract file", description = "Upload a file (PDF, images) for a contract")
    public ResponseEntity<ContractFileDto> uploadContractFile(
            @PathVariable UUID contractId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPrimary", required = false, defaultValue = "false") Boolean isPrimary,
            @RequestParam(value = "uploadedBy", required = false) UUID uploadedBy) {
        
        if (uploadedBy == null) {
            uploadedBy = UUID.randomUUID();
        }
        
        ContractFileDto fileDto = contractService.uploadContractFile(contractId, file, uploadedBy, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(fileDto);
    }

    @PostMapping("/{contractId}/files/multiple")
    @Operation(summary = "Upload multiple contract files", description = "Upload multiple files for a contract")
    public ResponseEntity<List<ContractFileDto>> uploadContractFiles(
            @PathVariable UUID contractId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "uploadedBy", required = false) UUID uploadedBy) {
        
        if (uploadedBy == null) {
            uploadedBy = UUID.randomUUID();
        }
        
        List<ContractFileDto> fileDtos = new java.util.ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            Boolean isPrimary = (i == 0);
            ContractFileDto fileDto = contractService.uploadContractFile(
                    contractId, files[i], uploadedBy, isPrimary);
            fileDtos.add(fileDto);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(fileDtos);
    }

    @GetMapping("/{contractId}/files")
    @Operation(summary = "Get contract files", description = "Get all files for a contract")
    public ResponseEntity<List<ContractFileDto>> getContractFiles(@PathVariable UUID contractId) {
        List<ContractFileDto> files = contractService.getContractFiles(contractId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{contractId}/files/{fileId}/view")
    @Operation(summary = "View contract file", description = "View contract file inline in browser")
    public ResponseEntity<Resource> viewContractFile(
            @PathVariable UUID contractId,
            @PathVariable UUID fileId,
            HttpServletRequest request) {
        
        Resource resource = contractService.viewContractFile(contractId, fileId);
        
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }
        
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{contractId}/files/{fileId}/download")
    @Operation(summary = "Download contract file", description = "Download contract file")
    public ResponseEntity<Resource> downloadContractFile(
            @PathVariable UUID contractId,
            @PathVariable UUID fileId,
            HttpServletRequest request) {
        
        Resource resource = contractService.downloadContractFile(contractId, fileId);
        
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }
        
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{contractId}/files/{fileId}")
    @Operation(summary = "Delete contract file", description = "Delete a contract file")
    public ResponseEntity<Void> deleteContractFile(
            @PathVariable UUID contractId,
            @PathVariable UUID fileId) {
        
        contractService.deleteContractFile(contractId, fileId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{contractId}/files/{fileId}/primary")
    @Operation(summary = "Set primary file", description = "Set a contract file as primary")
    public ResponseEntity<ContractFileDto> setPrimaryFile(
            @PathVariable UUID contractId,
            @PathVariable UUID fileId) {
        
        ContractFileDto fileDto = contractService.setPrimaryFile(contractId, fileId);
        return ResponseEntity.ok(fileDto);
    }

    // ===== Export contract to PDF and store as contract file =====
    @PostMapping("/{contractId}/export-pdf")
    @Operation(summary = "Export contract PDF from template and store as contract file")
    public ResponseEntity<?> exportContractPdf(
            @PathVariable UUID contractId,
            @RequestParam(defaultValue = "templates/contract_template.pdf") String templatePath,
            @RequestParam(defaultValue = "contract.pdf") String filename,
            @RequestParam(defaultValue = "true") boolean flatten,
            @RequestParam(value = "uploadedBy", required = false) UUID uploadedBy,
            @RequestBody(required = false) BuyerRequest buyer
    ) {
        if (uploadedBy == null) uploadedBy = UUID.randomUUID();
        log.info("[ExportPDF] contractId={}, templatePath={}, filename={}, flatten={}",
                contractId, templatePath, filename, flatten);
        try {
            // 1) Load contract
            ContractDto contract = contractService.getContractById(contractId);

            // 2) Map fields for PDF
            PdfFieldMapper.BuyerInfo buyerInfo = buyer == null ? null : new PdfFieldMapper.BuyerInfo(
                    buyer.name(), buyer.idNo(), buyer.idDate(), buyer.idPlace(),
                    buyer.residence(), buyer.address(), buyer.phone(), buyer.fax(),
                    buyer.bankAcc(), buyer.bankName(), buyer.taxCode()
            );
            Map<String, String> fields = PdfFieldMapper.mapFromContract(contract, buyerInfo);

            // 3) Fill PDF
            byte[] pdfBytes = pdfFormFillService.fillTemplate(templatePath, fields, flatten);

            // 4) Store as contract file
            MultipartFile file = new InMemoryMultipartFile(filename, filename, "application/pdf", pdfBytes);
            ContractFileDto fileDto = contractService.uploadContractFile(contractId, file, uploadedBy, false);
            log.info("[ExportPDF] Exported and stored file {}", fileDto.getFileName());
            return ResponseEntity.status(HttpStatus.CREATED).body(fileDto);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.error("[ExportPDF] Bad request: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
        } catch (Exception ex) {
            log.error("[ExportPDF] Unexpected error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected error"));
        }
    }

    public record BuyerRequest(
            String name,
            String idNo,
            String idDate,
            String idPlace,
            String residence,
            String address,
            String phone,
            String fax,
            String bankAcc,
            String bankName,
            String taxCode
    ) {}

    static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content != null ? content : new byte[0];
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content.clone(); }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    record ErrorResponse(int status, String message) {}
}

