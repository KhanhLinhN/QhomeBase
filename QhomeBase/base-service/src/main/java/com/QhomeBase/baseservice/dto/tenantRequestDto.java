package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.UUID;
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
   public class tenantRequestDto {
    @NotBlank
    @Size(max=50)
    @Pattern(regexp="^[A-Za-z0-9_-]+$")
    String code;
    @NotBlank @Size(max=100)
       String name;
    @Size(max=100) String contact;
    @Email
    @Size(max=255) String email;
       String status;
    @Size(max=500) String description;
   }


