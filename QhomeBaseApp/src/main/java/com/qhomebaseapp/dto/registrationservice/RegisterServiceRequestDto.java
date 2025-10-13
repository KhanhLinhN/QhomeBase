package com.qhomebaseapp.dto.registrationservice;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class RegisterServiceRequestDto {
    private String serviceType;
    private String description;
    private String phoneNumber;
    private String address;
    private String note;
    private String serviceCode;
}
