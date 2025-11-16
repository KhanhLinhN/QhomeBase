package com.QhomeBase.iamservice.service.imports;

import com.QhomeBase.iamservice.client.BaseServiceClient;
import com.QhomeBase.iamservice.dto.StaffImportResponse;
import com.QhomeBase.iamservice.dto.StaffImportRowResult;
import com.QhomeBase.iamservice.model.User;
import com.QhomeBase.iamservice.model.UserRole;
import com.QhomeBase.iamservice.model.imports.StaffImportRow;
import com.QhomeBase.iamservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffImportService {

    private static final int COL_USERNAME = 0;
    private static final int COL_EMAIL = 1;
    private static final int COL_ROLE = 2;
    private static final int COL_ACTIVE = 3;

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    private final UserService userService;
    private final BaseServiceClient baseServiceClient;

    public byte[] generateTemplateWorkbook() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("StaffImport");
            Row header = sheet.createRow(0);
            String[] headers = {"username", "email", "role", "active"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
            }

            Row sample1 = sheet.createRow(1);
            sample1.createCell(COL_USERNAME).setCellValue("staff.admin");
            sample1.createCell(COL_EMAIL).setCellValue("staff.admin@example.com");
            sample1.createCell(COL_ROLE).setCellValue("ADMIN");
            sample1.createCell(COL_ACTIVE).setCellValue(true);

            Row sample2 = sheet.createRow(2);
            sample2.createCell(COL_USERNAME).setCellValue("tech.support");
            sample2.createCell(COL_EMAIL).setCellValue("tech.support@example.com");
            sample2.createCell(COL_ROLE).setCellValue("TECHNICIAN,SUPPORTER");
            sample2.createCell(COL_ACTIVE).setCellValue(false);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo file template: " + e.getMessage(), e);
        }
    }

    public StaffImportResponse importStaffAccounts(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File import không được để trống");
        }
        if (file.getOriginalFilename() != null && !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Vui lòng sử dụng file Excel định dạng .xlsx");
        }

        List<StaffImportRowResult> rowResults = new ArrayList<>();
        int processedRows = 0;
        int successRows = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new IllegalArgumentException("Không tìm thấy dữ liệu trong file Excel");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                processedRows++;
                int excelRowNumber = i + 1;

                String username = getCellString(row, COL_USERNAME);
                String email = getCellString(row, COL_EMAIL);
                List<String> roleNames = extractRoleNames(getCellString(row, COL_ROLE));
                Boolean active = getCellBoolean(row, COL_ACTIVE);

                try {
                    StaffImportRow parsedRow = buildImportRow(excelRowNumber, username, email, roleNames, active);
                    User created = userService.createStaffAccount(
                            parsedRow.getUsername(),
                            parsedRow.getEmail(),
                            parsedRow.getRoles(),
                            parsedRow.getActive() == null || parsedRow.getActive()
                    );
                    baseServiceClient.syncStaffResident(created.getId(), created.getUsername(), created.getEmail(), null);
                    successRows++;
                    rowResults.add(new StaffImportRowResult(
                            excelRowNumber,
                            parsedRow.getUsername(),
                            parsedRow.getEmail(),
                            roleNames,
                            parsedRow.getActive(),
                            true,
                            created.getId(),
                            "Created"
                    ));
                } catch (Exception ex) {
                    log.warn("Failed to import staff row {}: {}", excelRowNumber, ex.getMessage());
                    rowResults.add(new StaffImportRowResult(
                            excelRowNumber,
                            username,
                            email,
                            roleNames,
                            active,
                            false,
                            null,
                            ex.getMessage()
                    ));
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể đọc file Excel: " + e.getMessage(), e);
        }

        int failureRows = processedRows - successRows;
        return new StaffImportResponse(processedRows, successRows, failureRows, rowResults);
    }

    private StaffImportRow buildImportRow(int rowNumber,
                                          String username,
                                          String email,
                                          List<String> roleNames,
                                          Boolean active) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Username (row " + rowNumber + ") không được để trống");
        }
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email (row " + rowNumber + ") không được để trống");
        }
        if (roleNames.isEmpty()) {
            throw new IllegalArgumentException("Role (row " + rowNumber + ") không được để trống");
        }
        if (active == null) {
            throw new IllegalArgumentException("Trạng thái 'active' không được phép rỗng (null)");
        }
        List<UserRole> roles = roleNames.stream()
                .map(roleName -> {
                    try {
                        return UserRole.valueOf(roleName.trim().toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException ex) {
                        throw new IllegalArgumentException("Role không hợp lệ tại dòng " + rowNumber + ": " + roleName);
                    }
                })
                .collect(Collectors.toList());

        return StaffImportRow.builder()
                .rowNumber(rowNumber)
                .username(username.trim())
                .email(email.trim())
                .roles(roles)
                .active(active)
                .build();
    }

    private String getCellString(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return "";
        }
        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private Boolean getCellBoolean(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case BOOLEAN -> cell.getBooleanCellValue();
            case STRING -> parseBoolean(cell.getStringCellValue());
            case NUMERIC -> cell.getNumericCellValue() != 0;
            default -> null;
        };
    }

    private Boolean parseBoolean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.matches("^(true|1|yes|y)$")) {
            return Boolean.TRUE;
        }
        if (normalized.matches("^(false|0|no|n)$")) {
            return Boolean.FALSE;
        }
        return null;
    }

    private List<String> extractRoleNames(String rawRoles) {
        if (!StringUtils.hasText(rawRoles)) {
            return List.of();
        }
        return Arrays.stream(rawRoles.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toList());
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = COL_USERNAME; i <= COL_ACTIVE; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK && StringUtils.hasText(getCellString(row, i))) {
                return false;
            }
        }
        return true;
    }
}


