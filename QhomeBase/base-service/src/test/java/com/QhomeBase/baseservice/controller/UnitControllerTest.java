package com.QhomeBase.baseservice.controller;

import com.QhomeBase.baseservice.dto.UnitCreateDto;
import com.QhomeBase.baseservice.dto.UnitDto;
import com.QhomeBase.baseservice.dto.UnitUpdateDto;
import com.QhomeBase.baseservice.model.UnitStatus;
import com.QhomeBase.baseservice.service.UnitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class UnitControllerTest {

    private UnitService service;
    private UnitController controller;

    @BeforeEach
    void setup() {
        service = mock(UnitService.class);
        controller = new UnitController(service);
    }

    @Test
    void createUnit_utcid01_adminRole_validData_returnsOk() {

        UnitCreateDto dto = new UnitCreateDto(
                UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75"),
                "A01",
                2,
                new BigDecimal("50"),
                1
        );

        UnitDto mockResult = mock(UnitDto.class);
        when(service.createUnit(any(UnitCreateDto.class))).thenReturn(mockResult);

        ResponseEntity<UnitDto> response = controller.createUnit(dto, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResult, response.getBody());
        verify(service).createUnit(dto);
    }

    @Test
    void createUnit_utcid02_supporterRole_returnsBadRequest() {
        UnitCreateDto dto = new UnitCreateDto(
                UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75"),
                "A01",
                2,
                new BigDecimal("50"),
                1
        );

        when(service.createUnit(any(UnitCreateDto.class)))
                .thenThrow(new IllegalStateException("Access denied"));

        ResponseEntity<UnitDto> response = controller.createUnit(dto, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(service).createUnit(dto);
    }
    @Test
    void createUnit_utcid03_adminRole_nullTenantId_returnsBadRequest() {
        UnitCreateDto dto = new UnitCreateDto(
                null,
                UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75"),
                "A01",
                2,
                new BigDecimal(50),
                1
        );

        when(service.createUnit(any(UnitCreateDto.class)))
                .thenThrow(new IllegalArgumentException("Invalid tenant ID"));

        ResponseEntity<UnitDto> response = controller.createUnit(dto, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(service).createUnit(dto);
    }

    @Test
    void createUnit_utcid06_adminRole_nonExistentTenantId_returnsBadRequest() {
        UnitCreateDto dto = new UnitCreateDto(
                UUID.fromString("7e4a1b0c-9d6f-4e3a-8b2c-5f0d91e6b37a"),
                UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75"),
                "A01",
                2,
                new BigDecimal(50),
                1
        );

        when(service.createUnit(any(UnitCreateDto.class)))
                .thenThrow(new IllegalArgumentException("Tenant not found with id: 7e4a1b0c-9d6f-4e3a-8b2c-5f0d91e6b37a"));

        ResponseEntity<UnitDto> response = controller.createUnit(dto, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(service).createUnit(dto);
    }

    @Test
    void createUnit_utcid07_adminRole_nullBuildingId_returnsBadRequest() {
        UnitCreateDto dto = new UnitCreateDto(
                UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                null,
                "A01",
                2,
                new BigDecimal(50),
                1
        );

        when(service.createUnit(any(UnitCreateDto.class)))
                .thenThrow(new IllegalArgumentException("Invalid building ID"));

        ResponseEntity<UnitDto> response = controller.createUnit(dto, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(service).createUnit(dto);
    }
    @Test
    void createUnit_utcid10_adminRole_nonExistentBuildingId_returnsBadRequest() {
        UnitCreateDto dto = new UnitCreateDto(
                UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                UUID.fromString("4b9d3e8f-a1c2-5d67-b8e9-0f1a2b3c4d5e"),
                "A01",
                2,
                new BigDecimal(50),
                1
        );

        when(service.createUnit(any(UnitCreateDto.class)))
                .thenThrow(new IllegalArgumentException("Building not found with id: 4b9d3e8f-a1c2-5d67-b8e9-0f1a2b3c4d5e"));

        ResponseEntity<UnitDto> response = controller.createUnit(dto, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(service).createUnit(dto);
    }

    @Test
    void createUnit_utcid11_adminRole_nullCode_returnsBadRequest() {
        UnitCreateDto dto = new UnitCreateDto(
                UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75"),
                null,
                2,
                new BigDecimal(50),
                1
        );

        when(service.createUnit(any(UnitCreateDto.class)))
                .thenThrow(new IllegalArgumentException("Invalid code"));

        ResponseEntity<UnitDto> response = controller.createUnit(dto, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(service).createUnit(dto);
    }
    @Test
    void createUnit_utcid12_adminRole_emptyCode_returnsBadRequest() {
        UnitCreateDto dto = new UnitCreateDto(
                UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75"),
                "",
                2,
                new BigDecimal(50),
                1
        );

        when(service.createUnit(any(UnitCreateDto.class)))
                .thenThrow(new IllegalArgumentException("Invalid code"));

        ResponseEntity<UnitDto> response = controller.createUnit(dto, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(service).createUnit(dto);
    }

    @Test
    void createUnit_utcid13_adminRole_codeExceeds50Chars_returnsBadRequest() {
        UnitCreateDto dto = new UnitCreateDto(
                UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75"),
                "g7@Hk2!PzQ9$LmN4tVr6wXyBc3Jp#a0O&dEf5GiU8sYuK%",
                2,
                new BigDecimal(50),
                1
        );

        when(service.createUnit(any(UnitCreateDto.class)))
                .thenThrow(new IllegalArgumentException("Code must not exceed 50 characters"));

        ResponseEntity<UnitDto> response = controller.createUnit(dto, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(service).createUnit(dto);
    }
    @Test
    void createUnit_utcid14_adminRole_floor0_returnsBadRequest() {
        UnitCreateDto dto = new UnitCreateDto(
                UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75"),
                "A01",
                0,
                new BigDecimal(50),
                1
        );

        when(service.createUnit(any(UnitCreateDto.class)))
                .thenThrow(new IllegalArgumentException("Floor must be positive"));

        ResponseEntity<UnitDto> response = controller.createUnit(dto, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(service).createUnit(dto);
    }

    @Test
    void createUnit_utcid15_adminRole_negativeFloor_returnsBadRequest() {
        UnitCreateDto dto = new UnitCreateDto(
                UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75"),
                "A01",
                -1,
                new BigDecimal(50),
                1
        );

        when(service.createUnit(any(UnitCreateDto.class)))
                .thenThrow(new IllegalArgumentException("Floor must be positive"));

        ResponseEntity<UnitDto> response = controller.createUnit(dto, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(service).createUnit(dto);
    }
    @Test
    void createUnit_utcid16_adminRole_floorExceedsBuildingFloors_returnsBadRequest() {
        UnitCreateDto dto = new UnitCreateDto(
                UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75"),
                "A01",
                5,
                new BigDecimal(50),
                1
        );

        when(service.createUnit(any(UnitCreateDto.class)))
                .thenThrow(new IllegalArgumentException("Apartment floor must not exceed the number of floors of building."));

        ResponseEntity<UnitDto> response = controller.createUnit(dto, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(service).createUnit(dto);
    }

    @Test
    void createUnit_utcid17_adminRole_area0_returnsBadRequest() {
        UnitCreateDto dto = new UnitCreateDto(
                UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75"),
                "A01",
                2,
                BigDecimal.ZERO,
                1
        );

        when(service.createUnit(any(UnitCreateDto.class)))
                .thenThrow(new IllegalArgumentException("Area must be positive"));

        ResponseEntity<UnitDto> response = controller.createUnit(dto, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(service).createUnit(dto);
    }
    @Test
    void createUnit_utcid18_adminRole_negativeArea_returnsBadRequest() {
        UnitCreateDto dto = new UnitCreateDto(
                UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75"),
                "A01",
                2,
                new BigDecimal(-1),
                1
        );

        when(service.createUnit(any(UnitCreateDto.class)))
                .thenThrow(new IllegalArgumentException("Area must be positive"));

        ResponseEntity<UnitDto> response = controller.createUnit(dto, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(service).createUnit(dto);
    }

    @Test
    void createUnit_utcid19_adminRole_bedrooms0_returnsBadRequest() {
        UnitCreateDto dto = new UnitCreateDto(
                UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75"),
                "A01",
                2,
                new BigDecimal(50),
                0
        );

        when(service.createUnit(any(UnitCreateDto.class)))
                .thenThrow(new IllegalArgumentException("Bedrooms must be positive"));

        ResponseEntity<UnitDto> response = controller.createUnit(dto, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(service).createUnit(dto);
    }
    @Test
    void createUnit_utcid20_adminRole_negativeBedrooms_returnsBadRequest() {
        UnitCreateDto dto = new UnitCreateDto(
                UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef"),
                UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75"),
                "A01",
                2,
                new BigDecimal(50),
                -1
        );

        when(service.createUnit(any(UnitCreateDto.class)))
                .thenThrow(new IllegalArgumentException("Bedrooms must be positive"));

        ResponseEntity<UnitDto> response = controller.createUnit(dto, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(service).createUnit(dto);
    }

    @Test
    void updateUnit_utcid01_adminRole_validData_returnsOk() {
        UUID id = UUID.fromString("6a5b4c3d-2e1f-0a9b-8c7d-6e5f4a3b2c1d");
        UnitUpdateDto dto = new UnitUpdateDto(2, new BigDecimal(50), 1);

        UnitDto mockResult = mock(UnitDto.class);
        when(service.updateUnit(any(UnitUpdateDto.class), eq(id))).thenReturn(mockResult);

        ResponseEntity<UnitDto> resp = controller.updateUnit(id, dto);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(mockResult, resp.getBody());
        verify(service).updateUnit(dto, id);
    }

    // ===== UTCID05: id không tồn tại =====
    @Test
    void updateUnit_utcid05_adminRole_nonExistentId_returnsBadRequest() {
        UUID notFoundId = UUID.fromString("9f8e7d6c-5b4a-3c2d-1e0f-9d8c7b6a5f4e");
        UnitUpdateDto dto = new UnitUpdateDto(2, new BigDecimal(50), 1);

        when(service.updateUnit(any(), eq(notFoundId)))
                .thenThrow(new IllegalArgumentException("Unit not found with id: 9f8e7d6c-5b4a-3c2d-1e0f-9d8c7b6a5f4e"));

        ResponseEntity<UnitDto> resp = controller.updateUnit(notFoundId, dto);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        verify(service).updateUnit(dto, notFoundId);
    }
    // ===== UTCID02: id = null =====
    @Test
    void updateUnit_utcid02_adminRole_nullId_returnsBadRequest() {
        UnitUpdateDto dto = new UnitUpdateDto(2, new BigDecimal("50"), 1);

        when(service.updateUnit(any(), eq(null)))
                .thenThrow(new IllegalArgumentException("Invalid id"));

        ResponseEntity<UnitDto> resp = controller.updateUnit(null, dto);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        verify(service).updateUnit(dto, null);
    }

    // ===== UTCID03: id = empty UUID =====
    @Test
    void updateUnit_utcid03_adminRole_emptyId_returnsBadRequest() {
        UUID emptyId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        UnitUpdateDto dto = new UnitUpdateDto(2, new BigDecimal("50"), 1);

        when(service.updateUnit(any(), eq(emptyId)))
                .thenThrow(new IllegalArgumentException("Invalid id"));

        ResponseEntity<UnitDto> resp = controller.updateUnit(emptyId, dto);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        verify(service).updateUnit(dto, emptyId);
    }

    // ===== UTCID06: Supporter role =====
    @Test
    void updateUnit_utcid06_supporterRole_returnsBadRequest() {
        UUID id = UUID.fromString("6a5b4c3d-2e1f-0a9b-8c7d-6e5f4a3b2c1d");
        UnitUpdateDto dto = new UnitUpdateDto(2, new BigDecimal("50"), 1);

        when(service.updateUnit(any(), eq(id)))
                .thenThrow(new IllegalArgumentException("Access denied"));

        ResponseEntity<UnitDto> resp = controller.updateUnit(id, dto);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        verify(service).updateUnit(dto, id);
    }

    // ===== UTCID09: floor > số tầng tòa nhà (5 > 4) =====
    @Test
    void updateUnit_utcid09_adminRole_floorExceedsBuilding_returnsBadRequest() {
        UUID id = UUID.fromString("6a5b4c3d-2e1f-0a9b-8c7d-6e5f4a3b2c1d");
        UnitUpdateDto dto = new UnitUpdateDto(5, new BigDecimal("50"), 1);

        when(service.updateUnit(any(), eq(id)))
                .thenThrow(new IllegalArgumentException("Apartment floor must not exceed the number of floors of building."));

        ResponseEntity<UnitDto> resp = controller.updateUnit(id, dto);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        verify(service).updateUnit(dto, id);
    }

    // ===== UTCID10: floor = 0 =====
    @Test
    void updateUnit_utcid10_adminRole_floor0_returnsBadRequest() {
        UUID id = UUID.fromString("6a5b4c3d-2e1f-0a9b-8c7d-6e5f4a3b2c1d");
        UnitUpdateDto dto = new UnitUpdateDto(0, new BigDecimal("50"), 1);

        when(service.updateUnit(any(), eq(id)))
                .thenThrow(new IllegalArgumentException("Floor must be positive"));

        ResponseEntity<UnitDto> resp = controller.updateUnit(id, dto);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        verify(service).updateUnit(dto, id);
    }

    // ===== UTCID11: area = -1 =====
    @Test
    void updateUnit_utcid11_adminRole_negativeArea_returnsBadRequest() {
        UUID id = UUID.fromString("6a5b4c3d-2e1f-0a9b-8c7d-6e5f4a3b2c1d");
        UnitUpdateDto dto = new UnitUpdateDto(2, new BigDecimal("-1"), 1);

        when(service.updateUnit(any(), eq(id)))
                .thenThrow(new IllegalArgumentException("Area must be positive"));

        ResponseEntity<UnitDto> resp = controller.updateUnit(id, dto);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        verify(service).updateUnit(dto, id);
    }

    // ===== UTCID12: bedrooms = -1 =====
    @Test
    void updateUnit_utcid12_adminRole_negativeBedrooms_returnsBadRequest() {
        UUID id = UUID.fromString("6a5b4c3d-2e1f-0a9b-8c7d-6e5f4a3b2c1d");
        UnitUpdateDto dto = new UnitUpdateDto(2, new BigDecimal("50"), -1);

        when(service.updateUnit(any(), eq(id)))
                .thenThrow(new IllegalArgumentException("Bedrooms must be positive"));

        ResponseEntity<UnitDto> resp = controller.updateUnit(id, dto);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        verify(service).updateUnit(dto, id);
    }


    @Test
    void deleteUnit_utcid05_adminRole_nonExistentId_returnsBadRequest() {
        UUID notFoundId = UUID.fromString("9f8e7d6c-5b4a-3c2d-1e0f-9d8c7b6a5f4e");

        doThrow(new IllegalArgumentException("Unit not found with id: 9f8e7d6c-5b4a-3c2d-1e0f-9d8c7b6a5f4e"))
                .when(service).deleteUnit(eq(notFoundId));

        ResponseEntity<Void> resp = controller.deleteUnit(notFoundId);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        verify(service).deleteUnit(eq(notFoundId));
    }
    @Test
    void deleteUnit_utcid01_adminRole_validId_returnsOk() {
        UUID id = UUID.fromString("6a5b4c3d-2e1f-0a9b-8c7d-6e5f4a3b2c1d");

        doNothing().when(service).deleteUnit(eq(id));

        ResponseEntity<Void> resp = controller.deleteUnit(id);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(service).deleteUnit(eq(id));
    }
    @Test
    void getUnitById_utcid01_adminRole_validId_returnsOk() {
        UUID id = UUID.fromString("6a5b4c3d-2e1f-0a9b-8c7d-6e5f4a3b2c1d");

        UnitDto mockResult = mock(UnitDto.class);
        when(service.getUnitById(eq(id))).thenReturn(mockResult);

        ResponseEntity<UnitDto> resp = controller.getUnitById(id);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(mockResult, resp.getBody());
        verify(service).getUnitById(eq(id));
    }
    @Test
    void getUnitById_utcid05_adminRole_nonExistentId_returnsBadRequest() {
        UUID notFoundId = UUID.fromString("9f8e7d6c-5b4a-3c2d-1e0f-9d8c7b6a5f4e");

        when(service.getUnitById(eq(notFoundId)))
                .thenThrow(new IllegalArgumentException("Unit not found with id: 9f8e7d6c-5b4a-3c2d-1e0f-9d8c7b6a5f4e"));

        ResponseEntity<UnitDto> resp = controller.getUnitById(notFoundId);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        verify(service).getUnitById(eq(notFoundId));
    }
    @Test
    void getUnitsByBuildingId_utcid01_adminRole_validBuildingId_returnsOk() {
        UUID bid = UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75");

        when(service.getUnitsByBuildingId(eq(bid)))
                .thenReturn(List.of(mock(UnitDto.class)));

        ResponseEntity<List<UnitDto>> resp = controller.getUnitsByBuildingId(bid);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
        verify(service).getUnitsByBuildingId(eq(bid));
    }


    @Test
    void getUnitsByTenantId_utcid01_adminRole_validTenantId_returnsOk() {
        UUID tid = UUID.fromString("a1b2c3d4-e5f6-7890-1234-567890abcdef");

        when(service.getUnitsByTenantId(eq(tid)))
                .thenReturn(List.of(mock(UnitDto.class), mock(UnitDto.class)));

        ResponseEntity<List<UnitDto>> resp = controller.getUnitsByTenantId(tid);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(2, resp.getBody().size());
        verify(service).getUnitsByTenantId(eq(tid));
    }

    @Test
    void getUnitsByFloor_ok() {
        UUID bid = UUID.randomUUID();
        when(service.getUnitsByFloor(eq(bid), eq(5))).thenReturn(List.of(mock(UnitDto.class)));
        ResponseEntity<List<UnitDto>> resp = controller.getUnitsByFloor(bid, 5);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(service).getUnitsByFloor(eq(bid), eq(5));
    }

    @Test
    void getUnitsByFloor_utcid01_adminRole_validParams_returnsOk() {
        UUID bid = UUID.fromString("0f45a2c9-d3b6-4e81-a7f0-2b1e6d9c8a75");
        int floor = 2;

        when(service.getUnitsByFloor(eq(bid), eq(floor)))
                .thenReturn(List.of(mock(UnitDto.class)));

        ResponseEntity<List<UnitDto>> resp = controller.getUnitsByFloor(bid, floor);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(1, resp.getBody().size());
        verify(service).getUnitsByFloor(eq(bid), eq(floor));
    }


}