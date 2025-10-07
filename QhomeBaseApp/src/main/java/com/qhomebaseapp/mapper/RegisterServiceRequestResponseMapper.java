package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestResponseDto;
import com.qhomebaseapp.model.RegisterServiceRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface RegisterServiceRequestResponseMapper {
    RegisterServiceRequestResponseMapper INSTANCE = Mappers.getMapper(RegisterServiceRequestResponseMapper.class);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    RegisterServiceRequestResponseDto toDto(RegisterServiceRequest entity);
}
