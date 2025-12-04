package com.QhomeBase.baseservice.service.imports;

import com.QhomeBase.baseservice.dto.UnitCreateDto;
import com.QhomeBase.baseservice.dto.imports.UnitImportResponse;
import com.QhomeBase.baseservice.dto.imports.UnitImportRowResult;
import com.QhomeBase.baseservice.model.Building;
import com.QhomeBase.baseservice.model.Unit;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import com.QhomeBase.baseservice.repository.UnitRepository;
import com.QhomeBase.baseservice.service.UnitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@org.springframework.transaction.annotation.Transactional
public class UnitImportService {
    private final BuildingRepository buildingRepository;
    private final UnitRepository unitRepository;
    private final UnitService unitService;

    public UnitImportResponse importUnits(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File trống");
        }
        if (!Objects.requireNonNull(file.getOriginalFilename()).toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ .xlsx");
        }
        UnitImportResponse response = UnitImportResponse.builder().build();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("Không tìm thấy sheet");
            }
            if (sheet.getLastRowNum() < 1) {
                return response;
            }
            response.setTotalRows(sheet.getLastRowNum());
            Row header = sheet.getRow(0);
            int idxBuildingCode = findColumnIndex(header, "buildingCode");
            int idxFloor = findColumnIndex(header, "floor");
            int idxArea = findColumnIndex(header, "areaM2");
            int idxBedrooms = findColumnIndex(header, "bedrooms");
            if (idxBuildingCode < 0) {
                throw new IllegalArgumentException("Thiếu cột buildingCode");
            }
            if (idxFloor < 0 || idxArea < 0 || idxBedrooms < 0) {
                throw new IllegalArgumentException("Thiếu các cột bắt buộc: floor, areaM2, bedrooms");
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r == null) continue;
                int excelRow = i;
                String buildingCode = readString(r, idxBuildingCode);
                Integer floor = readInt(r, idxFloor);
                BigDecimal areaM2 = readDecimal(r, idxArea);
                Integer bedrooms = readInt(r, idxBedrooms);
                try {
                    Building building = resolveBuilding(buildingCode, excelRow);
                    UUID buildingId = building.getId();
                    validateUnitData(floor, areaM2, bedrooms, building, excelRow);
                    var dto = unitService.createUnit(new UnitCreateDto(buildingId, null, floor, areaM2, bedrooms));
                    Unit created = unitRepository.findById(dto.id()).orElseThrow();
                    Unit createdWithBuilding = unitRepository.findByIdWithBuilding(created.getId());

                    response.getRows().add(UnitImportRowResult.builder()
                            .rowNumber(excelRow)
                            .success(true)
                            .message("OK")
                            .unitId(createdWithBuilding.getId().toString())
                            .buildingId(createdWithBuilding.getBuilding().getId().toString())
                            .buildingCode(createdWithBuilding.getBuilding().getCode())
                            .code(createdWithBuilding.getCode())
                            .build());
                    response.setSuccessCount(response.getSuccessCount() + 1);
                } catch (Exception ex) {
                    log.warn("Import unit lỗi tại dòng {}: {}", excelRow, ex.getMessage());
                    response.getRows().add(UnitImportRowResult.builder()
                            .rowNumber(excelRow)
                            .success(false)
                            .message(ex.getMessage())
                            .build());
                    response.setErrorCount(response.getErrorCount() + 1);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được file Excel", e);
        }
        return response;
    }

    public byte[] generateTemplateWorkbook() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("units");
            Row header = sh.createRow(0);
            header.createCell(0).setCellValue("buildingCode");
            header.createCell(1).setCellValue("floor");
            header.createCell(2).setCellValue("areaM2");
            header.createCell(3).setCellValue("bedrooms");

            Row sample1 = sh.createRow(1);
            sample1.createCell(0).setCellValue("A");
            sample1.createCell(1).setCellValue(1);
            sample1.createCell(2).setCellValue(45.5);
            sample1.createCell(3).setCellValue(2);

            Row sample2 = sh.createRow(2);
            sample2.createCell(0).setCellValue("A");
            sample2.createCell(1).setCellValue(2);
            sample2.createCell(2).setCellValue(60.0);
            sample2.createCell(3).setCellValue(3);

            for (int c = 0; c <= 3; c++) sh.autoSizeColumn(c);
            wb.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được template", e);
        }
    }

    private Building resolveBuilding(String buildingCode, int rowNumber) {
        if (buildingCode == null || buildingCode.isBlank()) {
            throw new IllegalArgumentException("BuildingCode (row " + rowNumber + ") không được để trống");
        }
        Optional<Building> found = buildingRepository.findAllByOrderByCodeAsc()
                .stream()
                .filter(b -> buildingCode.trim().equalsIgnoreCase(b.getCode()))
                .findFirst();
        if (found.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy Building với code: " + buildingCode.trim() + " (row " + rowNumber + ")");
        }
        return found.get();
    }

    private int findColumnIndex(Row header, String name) {
        if (header == null) return -1;
        String target = name.toLowerCase(Locale.ROOT).trim();
        short last = header.getLastCellNum();
        for (int i = 0; i < last; i++) {
            Cell cell = header.getCell(i);
            if (cell == null) continue;
            String v = cell.getStringCellValue();
            if (v != null && v.trim().toLowerCase(Locale.ROOT).equals(target)) {
                return i;
            }
        }
        return -1;
    }

    private String readString(Row r, int idx) {
        if (idx < 0) return null;
        Cell c = r.getCell(idx);
        if (c == null) return null;
        c.setCellType(CellType.STRING);
        String v = c.getStringCellValue();
        return v != null ? v.trim() : null;
    }

    private Integer readInt(Row r, int idx) {
        if (idx < 0) return null;
        Cell c = r.getCell(idx);
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC) {
            return (int) Math.round(c.getNumericCellValue());
        }
        c.setCellType(CellType.STRING);
        String v = c.getStringCellValue();
        if (v == null || v.isBlank()) return null;
        try {
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Giá trị không hợp lệ (số nguyên): " + v);
        }
    }

    private java.math.BigDecimal readDecimal(Row r, int idx) {
        if (idx < 0) return null;
        Cell c = r.getCell(idx);
        if (c == null) return null;
        if (c.getCellType() == CellType.NUMERIC) {
            return java.math.BigDecimal.valueOf(c.getNumericCellValue());
        }
        c.setCellType(CellType.STRING);
        String v = c.getStringCellValue();
        if (v == null || v.isBlank()) return null;
        try {
            return new java.math.BigDecimal(v.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Giá trị không hợp lệ (số thập phân): " + v);
        }
    }

    private void validateUnitData(Integer floor, BigDecimal areaM2, Integer bedrooms, Building building, int rowNumber) {
        validateFloor(floor, building, rowNumber);
        validateAreaM2(areaM2, rowNumber);
        validateBedrooms(bedrooms, rowNumber);
    }

    private void validateFloor(Integer floor, Building building, int rowNumber) {
        if (floor == null) {
            throw new IllegalArgumentException("Floor (row " + rowNumber + ") không được để trống");
        }
        if (floor < 0) {
            throw new IllegalArgumentException("Floor (row " + rowNumber + ") phải lớn hơn hoặc bằng 0");
        }
        if (building.getNumberOfFloors() != null && floor > building.getNumberOfFloors()) {
            throw new IllegalArgumentException(
                    String.format("Floor %d (row %d) vượt quá số tầng của tòa nhà %s (%d tầng)", 
                            floor, rowNumber, building.getCode(), building.getNumberOfFloors()));
        }
        if (floor > 200) {
            throw new IllegalArgumentException("Floor (row " + rowNumber + ") không được vượt quá 200");
        }
    }

    private void validateAreaM2(BigDecimal areaM2, int rowNumber) {
        if (areaM2 == null) {
            throw new IllegalArgumentException("AreaM2 (row " + rowNumber + ") không được để trống");
        }
        if (areaM2.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("AreaM2 (row " + rowNumber + ") phải lớn hơn 0");
        }
        if (areaM2.compareTo(new BigDecimal("10000")) > 0) {
            throw new IllegalArgumentException("AreaM2 (row " + rowNumber + ") không được vượt quá 10000 m²");
        }
    }

    private void validateBedrooms(Integer bedrooms, int rowNumber) {
        if (bedrooms == null) {
            throw new IllegalArgumentException("Bedrooms (row " + rowNumber + ") không được để trống");
        }
        if (bedrooms < 0) {
            throw new IllegalArgumentException("Bedrooms (row " + rowNumber + ") phải lớn hơn hoặc bằng 0");
        }
        if (bedrooms > 20) {
            throw new IllegalArgumentException("Bedrooms (row " + rowNumber + ") không được vượt quá 20");
        }
    }
}


