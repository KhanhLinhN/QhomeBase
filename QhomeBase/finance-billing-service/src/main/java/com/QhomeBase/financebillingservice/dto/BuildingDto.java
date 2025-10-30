package com.QhomeBase.financebillingservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuildingDto {
    UUID id;
    UUID tenantId;
    @JsonProperty("codeName")
    String code;
    String name;
    String address;
    Instant createdAt;
    Instant updatedAt;
}

