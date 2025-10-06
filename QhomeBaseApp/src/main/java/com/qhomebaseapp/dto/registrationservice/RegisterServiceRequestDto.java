package com.qhomebaseapp.dto.registrationservice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterServiceRequestDto {
    private Long userId;
    private String serviceType;
    private String note;
}
