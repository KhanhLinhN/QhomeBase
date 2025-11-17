package com.QhomeBase.baseservice.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingImportResponse {
    @Builder.Default
    private int totalRows = 0;
    @Builder.Default
    private int successCount = 0;
    @Builder.Default
    private int errorCount = 0;
    @Builder.Default
    private List<BuildingImportRowResult> rows = new ArrayList<>();
}


