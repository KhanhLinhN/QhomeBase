package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.RegisterServiceRequestDto;
import com.qhomebaseapp.model.RegisterServiceRequest;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RegisterServiceRequestMapper {
    RegisterServiceRequestMapper INSTANCE = Mappers.getMapper(RegisterServiceRequestMapper.class);
    RegisterServiceRequest toEntity(RegisterServiceRequestDto dto);
}
