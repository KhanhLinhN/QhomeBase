package com.QhomeBase.customerinteractionservice.dto;

import java.time.Instant;
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
    String record_type;
    UUID record_id;
    UUID staff_in_charge;
    String content;
    Instant created_at;
}
