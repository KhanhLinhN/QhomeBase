package com.QhomeBase.baseservice.service.imports;

import com.QhomeBase.baseservice.dto.BuildingCreateReq;
import com.QhomeBase.baseservice.dto.imports.BuildingImportResponse;
import com.QhomeBase.baseservice.dto.imports.BuildingImportRowResult;
import com.QhomeBase.baseservice.model.Building;
import com.QhomeBase.baseservice.model.imports.BuildingImportRow;
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
import java.util.UUID;

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
            int idxCode = findColumnIndex(header, "code");
            int idxName = findColumnIndex(header, "name");
            int idxAddress = findColumnIndex(header, "address");
            if (idxName < 0) {
                throw new IllegalArgumentException("Thiếu cột name");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r == null) continue;
                int excelRow = i + 1;
                String code = readString(r, idxCode);
                String name = readString(r, idxName);
                String address = readString(r, idxAddress);

                try {
                    if (name == null || name.isBlank()) {
                        throw new IllegalArgumentException("Tên building trống");
                    }
                    Building saved;
                    if (code != null && !code.isBlank()) {
                        Building b = Building.builder()
                                .code(code.trim())
                                .name(name.trim())
                                .address(address)
                                .createdBy(createdBy != null ? createdBy : "import")
                                .build();
                        saved = buildingRepository.save(b);
                    } else {
                        var dto = buildingService.createBuilding(new BuildingCreateReq(name, address), createdBy != null ? createdBy : "import");
                        saved = buildingRepository.getBuildingById(dto.id());
                    }

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
            header.createCell(0).setCellValue("code");
            header.createCell(1).setCellValue("name");
            header.createCell(2).setCellValue("address");

            Row sample1 = sh.createRow(1);
            sample1.createCell(0).setCellValue("A");
            sample1.createCell(1).setCellValue("Tòa A");
            sample1.createCell(2).setCellValue("123 Đường ABC, Quận 1");

            Row sample2 = sh.createRow(2);
            sample2.createCell(0).setCellValue("");
            sample2.createCell(1).setCellValue("Tòa B");
            sample2.createCell(2).setCellValue("456 Đường DEF, Quận 2");

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
        c.setCellType(CellType.STRING);
        String v = c.getStringCellValue();
        return v != null ? v.trim() : null;
    }
}


