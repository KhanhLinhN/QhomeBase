package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.residentcard.ResidentCardRegistrationDto;
import com.qhomebaseapp.model.ResidentCardRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface ResidentCardRegistrationMapper {
    ResidentCardRegistrationMapper INSTANCE = Mappers.getMapper(ResidentCardRegistrationMapper.class);
    ResidentCardRegistration toEntity(ResidentCardRegistrationDto dto);
}

