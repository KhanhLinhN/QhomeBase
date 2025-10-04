package com.QhomeBase.baseservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;
@Getter @Setter
public class tenantUpdateDto {

    String name;
    String contact;
    String email;
    String status;
    String description;
    boolean isDeleted;

}
