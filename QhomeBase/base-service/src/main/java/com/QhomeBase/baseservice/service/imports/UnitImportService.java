package com.QhomeBase.baseservice.service.imports;

import com.QhomeBase.baseservice.dto.imports.UnitImportResponse;
import com.QhomeBase.baseservice.dto.imports.UnitImportRowResult;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class UnitImportService {
    private final UnitImportHelper unitImportHelper;

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
                    UnitImportRowResult rowResult = unitImportHelper.importSingleUnit(
                            buildingCode, floor, areaM2, bedrooms, excelRow);
                    response.getRows().add(rowResult);
                    if (rowResult.isSuccess()) {
                        response.setSuccessCount(response.getSuccessCount() + 1);
                    } else {
                        response.setErrorCount(response.getErrorCount() + 1);
                    }
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

}


