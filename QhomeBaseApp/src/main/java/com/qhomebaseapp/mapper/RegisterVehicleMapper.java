package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.registervehicle.RegisterVehicleRequestDto;
import com.qhomebaseapp.model.RegisterVehicleRequest;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RegisterVehicleMapper {
    RegisterVehicleMapper INSTANCE = Mappers.getMapper(RegisterVehicleMapper.class);
    RegisterVehicleRequest toEntity(RegisterVehicleRequestDto dto);
}

