package com.QhomeBase.customerinteractionservice.dto.news;

import com.QhomeBase.customerinteractionservice.model.TargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsTargetDto {

    private UUID id;
    private TargetType targetType;
    private UUID buildingId;
    private String buildingName;
}

