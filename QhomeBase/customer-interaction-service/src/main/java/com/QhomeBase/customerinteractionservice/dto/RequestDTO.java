package com.QhomeBase.customerinteractionservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestDTO {
    UUID id;
    String requestCode;
    UUID residentId;
    String residentName;
    String imagePath;
    String title;
    String content;
    String status;
    String priority;
    String createdAt;
    String updatedAt;
}
