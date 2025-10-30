package com.QhomeBase.baseservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TenantUpdateDto {

    String name;
    String contact;
    String email;
    String address;
    String status;
    String description;
    boolean isDeleted;

}
