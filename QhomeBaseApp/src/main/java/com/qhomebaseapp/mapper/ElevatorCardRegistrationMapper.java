package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.elevatorcard.ElevatorCardRegistrationDto;
import com.qhomebaseapp.model.ElevatorCardRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface ElevatorCardRegistrationMapper {
    ElevatorCardRegistrationMapper INSTANCE = Mappers.getMapper(ElevatorCardRegistrationMapper.class);
    ElevatorCardRegistration toEntity(ElevatorCardRegistrationDto dto);
}

