package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.imports.BuildingImportResponse;
import com.QhomeBase.baseservice.service.imports.BuildingImportService;
import com.QhomeBase.baseservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/buildings/import")
@RequiredArgsConstructor
@Slf4j
public class BuildingImportController {

    private final BuildingImportService buildingImportService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authz.canCreateBuilding()")
    public ResponseEntity<BuildingImportResponse> importBuildings(
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) {
        try {
            String createdBy = "import";
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal up) {
                createdBy = up.username();
            }
            BuildingImportResponse response = buildingImportService.importBuildings(file, createdBy);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[ImportBuilding] Bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("[ImportBuilding] Unexpected error", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping(value = "/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("@authz.canViewBuildings()")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] bytes = buildingImportService.generateTemplateWorkbook();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"building_import_template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}


