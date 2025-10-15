package com.QhomeBase.baseservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
   public class TenantRequestDto {
    @NotBlank
    @Size(max=50)
    @Pattern(regexp="^[A-Za-z0-9_-]+$")  String code;
    @NotBlank @Size(min = 2, max = 100)   String name;
    @Pattern(regexp = "^[0-9+\\-\\s()]+$", message = "Phone number format is invalid")
    @Pattern(regexp = "^[+0-9()\\-\\s]+$", message = "phone number format is invalid")
    @Size(min = 10, max = 20, message = "phone number length must be 10..20")
    String contact;
    @Email(message = "email is invalid")
    @Pattern(
            regexp = "^(?=.{1,255}$)(?!.*\\.\\.)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "email is invalid"
    )

    @Size(max=255) String email;
    @Size(max=500) String address;

    String status;
    @Size(max=500) String description;
   }


