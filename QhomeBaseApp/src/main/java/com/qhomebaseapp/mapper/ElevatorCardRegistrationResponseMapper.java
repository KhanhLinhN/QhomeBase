package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.elevatorcard.ElevatorCardRegistrationResponseDto;
import com.qhomebaseapp.model.ElevatorCardRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ElevatorCardRegistrationResponseMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    ElevatorCardRegistrationResponseDto toDto(ElevatorCardRegistration entity);
}

