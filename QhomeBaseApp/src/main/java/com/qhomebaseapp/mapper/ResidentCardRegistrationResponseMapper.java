package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.residentcard.ResidentCardRegistrationResponseDto;
import com.qhomebaseapp.model.ResidentCardRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ResidentCardRegistrationResponseMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    ResidentCardRegistrationResponseDto toDto(ResidentCardRegistration entity);
}

