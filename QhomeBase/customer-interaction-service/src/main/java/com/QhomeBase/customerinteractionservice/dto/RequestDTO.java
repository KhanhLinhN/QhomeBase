package com.QhomeBase.customerinteractionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestDTO {
    UUID id;
    UUID tenantId;
    UUID residentId;
    String resident_name;
    String image_path;
    String title;
    String content;
    String status;
    String priority;
    Instant created_at;
    Instant updated_at;
}
