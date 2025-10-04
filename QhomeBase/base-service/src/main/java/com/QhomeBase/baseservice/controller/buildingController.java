package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.model.building;
import com.QhomeBase.baseservice.service.buildingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/buildings")
public class buildingController {

    @Autowired
    private buildingService service;

    @GetMapping
    public ResponseEntity<List<building>> findAll(@RequestParam(required = false) UUID tenantId) {
        if (tenantId == null) {
            return ResponseEntity.badRequest().build();
        }


        List<building> mockBuildings = createMockBuildings(tenantId);
        return ResponseEntity.ok(mockBuildings);
    }

    private List<building> createMockBuildings(UUID tenantId) {
        String tenantIdStr = tenantId.toString();


        if (tenantIdStr.equals("123e4567-e89b-12d3-a456-426614174000")) {
            return List.of(
                    building.builder()
                            .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                            .tenantId(tenantId)
                            .code("BLD001")
                            .name("Tòa A - Chung cư ABC")
                            .address("123 Đường ABC, Quận 1, TP.HCM")
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build(),
                    building.builder()
                            .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                            .tenantId(tenantId)
                            .code("BLD002")
                            .name("Tòa B - Chung cư ABC")
                            .address("123 Đường ABC, Quận 1, TP.HCM")
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build(),
                    building.builder()
                            .id(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                            .tenantId(tenantId)
                            .code("BLD003")
                            .name("Tòa C - Chung cư ABC")
                            .address("123 Đường ABC, Quận 1, TP.HCM")
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );
        } else if (tenantIdStr.equals("550e8400-e29b-41d4-a716-446655440000")) {
            return List.of(
                    building.builder()
                            .id(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                            .tenantId(tenantId)
                            .code("BLD001")
                            .name("Tòa A - Khu đô thị XYZ")
                            .address("456 Đường XYZ, Quận 2, TP.HCM")
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build(),
                    building.builder()
                            .id(UUID.fromString("55555555-5555-5555-5555-555555555555"))
                            .tenantId(tenantId)
                            .code("BLD002")
                            .name("Tòa B - Khu đô thị XYZ")
                            .address("456 Đường XYZ, Quận 2, TP.HCM")
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );
        } else {

            return List.of(
                    building.builder()
                            .id(UUID.randomUUID())
                            .tenantId(tenantId)
                            .code("BLD001")
                            .name("Tòa A - Default")
                            .address("Default Address")
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build()
            );
        }
    }
}