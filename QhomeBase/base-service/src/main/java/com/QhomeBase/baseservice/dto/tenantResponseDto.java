package com.QhomeBase.baseservice.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;
@Data
@NoArgsConstructor @AllArgsConstructor
@Builder
public class tenantResponseDto {
        UUID id;;
        String code;
        String name;
        String contact;
        String email;
        String status;
        String description;
        Instant createdAt;
        Instant updatedAt;
        String createdBy;
        String updatedBy;
        boolean isDeleted;


}
