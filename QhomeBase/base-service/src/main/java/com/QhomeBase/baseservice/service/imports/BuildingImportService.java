package com.QhomeBase.baseservice.service.imports;

import com.QhomeBase.baseservice.dto.BuildingCreateReq;
import com.QhomeBase.baseservice.dto.imports.BuildingImportResponse;
import com.QhomeBase.baseservice.dto.imports.BuildingImportRowResult;
import com.QhomeBase.baseservice.model.Building;
import com.QhomeBase.baseservice.repository.BuildingRepository;
import com.QhomeBase.baseservice.service.BuildingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildingImportService {

    private final BuildingRepository buildingRepository;
    private final BuildingService buildingService;

    public BuildingImportResponse importBuildings(MultipartFile file, String createdBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File trống");
        }
        if (!Objects.requireNonNull(file.getOriginalFilename()).toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Chỉ hỗ trợ .xlsx");
        }

        BuildingImportResponse response = BuildingImportResponse.builder().build();
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
            int idxName = findColumnIndex(header, "name");
            int idxAddress = findColumnIndex(header, "address");
            int idxNumberOfFloors = findColumnIndex(header, "numberOfFloors");
            if (idxName < 0) {
                throw new IllegalArgumentException("Thiếu cột name");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r == null) continue;
                int excelRow = i;
                String name = readString(r, idxName);
                String address = readString(r, idxAddress);
                Integer numberOfFloors = readInteger(r, idxNumberOfFloors);

                try {
                    String trimmedName = name != null ? name.trim() : null;
                    String trimmedAddress = address != null ? address.trim() : null;
                    
                    validateBuildingName(trimmedName, excelRow);
                    validateBuildingAddress(trimmedAddress, excelRow);
                    validateNumberOfFloors(numberOfFloors, excelRow);
                    
                    var dto = buildingService.createBuilding(new BuildingCreateReq(trimmedName, trimmedAddress, numberOfFloors), createdBy != null ? createdBy : "import");
                    Building saved = buildingRepository.getBuildingById(dto.id());

                    response.getRows().add(BuildingImportRowResult.builder()
                            .rowNumber(excelRow)
                            .success(true)
                            .message("OK")
                            .buildingId(saved.getId().toString())
                            .code(saved.getCode())
                            .name(saved.getName())
                            .build());
                    response.setSuccessCount(response.getSuccessCount() + 1);
                } catch (Exception ex) {
                    log.warn("Import building lỗi tại dòng {}: {}", excelRow, ex.getMessage());
                    response.getRows().add(BuildingImportRowResult.builder()
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
            Sheet sh = wb.createSheet("buildings");
            Row header = sh.createRow(0);
            header.createCell(0).setCellValue("name");
            header.createCell(1).setCellValue("address");
            header.createCell(2).setCellValue("numberOfFloors");

            Row sample1 = sh.createRow(1);
            sample1.createCell(0).setCellValue("Tòa A");
            sample1.createCell(1).setCellValue("123 Đường ABC, Quận 1");
            sample1.createCell(2).setCellValue(10);

            Row sample2 = sh.createRow(2);
            sample2.createCell(0).setCellValue("Tòa B");
            sample2.createCell(1).setCellValue("456 Đường DEF, Quận 2");
            sample2.createCell(2).setCellValue(15);

            for (int c = 0; c <= 2; c++) sh.autoSizeColumn(c);
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
        String v;
        if (c.getCellType() == CellType.STRING) {
            v = c.getStringCellValue();
        } else if (c.getCellType() == CellType.NUMERIC) {
            v = String.valueOf((long) c.getNumericCellValue());
        } else {
            DataFormatter formatter = new DataFormatter();
            v = formatter.formatCellValue(c);
        }
        return v != null ? v.trim() : null;
    }

    private void validateBuildingName(String name, int rowNumber) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên building (row " + rowNumber + ") không được để trống");
        }
        
        if (name.length() > 255) {
            throw new IllegalArgumentException("Tên building (row " + rowNumber + ") không được vượt quá 255 ký tự");
        }
        
        if (name.length() < 2) {
            throw new IllegalArgumentException("Tên building (row " + rowNumber + ") phải có ít nhất 2 ký tự");
        }
    }

    private void validateBuildingAddress(String address, int rowNumber) {
        if (address != null && !address.isBlank()) {
            if (address.length() > 500) {
                throw new IllegalArgumentException("Địa chỉ (row " + rowNumber + ") không được vượt quá 500 ký tự");
            }
            
            if (address.length() < 5) {
                throw new IllegalArgumentException("Địa chỉ (row " + rowNumber + ") phải có ít nhất 5 ký tự");
            }
        }
    }

    private Integer readInteger(Row r, int idx) {
        if (idx < 0) return null;
        Cell c = r.getCell(idx);
        if (c == null) return null;
        try {
            if (c.getCellType() == CellType.NUMERIC) {
                double numValue = c.getNumericCellValue();
                return (int) numValue;
            } else if (c.getCellType() == CellType.STRING) {
                String strValue = c.getStringCellValue();
                if (strValue == null || strValue.trim().isEmpty()) {
                    return null;
                }
                return Integer.parseInt(strValue.trim());
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private void validateNumberOfFloors(Integer numberOfFloors, int rowNumber) {
        if (numberOfFloors != null && numberOfFloors <= 0) {
            throw new IllegalArgumentException("Số tầng (row " + rowNumber + ") phải lớn hơn 0");
        }
    }
}


