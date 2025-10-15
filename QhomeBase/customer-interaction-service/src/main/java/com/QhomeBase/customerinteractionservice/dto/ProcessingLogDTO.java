package com.QhomeBase.customerinteractionservice.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessingLogDTO {
    UUID id;
    String recordType;
    UUID recordId;
    UUID staffInCharge;
    String content;
    String requestStatus;
    String logType;
    String staffInChargeName;
    String createdAt;
}
