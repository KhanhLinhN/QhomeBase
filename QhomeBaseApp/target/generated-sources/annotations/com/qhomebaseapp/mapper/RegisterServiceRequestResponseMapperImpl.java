package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestResponseDto;
import com.qhomebaseapp.model.RegisterServiceRequest;
import com.qhomebaseapp.model.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-31T21:19:50+0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251023-0518, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class RegisterServiceRequestResponseMapperImpl implements RegisterServiceRequestResponseMapper {

    @Override
    public RegisterServiceRequestResponseDto toDto(RegisterServiceRequest entity) {
        if ( entity == null ) {
            return null;
        }

        RegisterServiceRequestResponseDto.RegisterServiceRequestResponseDtoBuilder registerServiceRequestResponseDto = RegisterServiceRequestResponseDto.builder();

        registerServiceRequestResponseDto.userId( entityUserId( entity ) );
        registerServiceRequestResponseDto.userEmail( entityUserEmail( entity ) );
        registerServiceRequestResponseDto.createdAt( entity.getCreatedAt() );
        registerServiceRequestResponseDto.id( entity.getId() );
        registerServiceRequestResponseDto.licensePlate( entity.getLicensePlate() );
        registerServiceRequestResponseDto.note( entity.getNote() );
        registerServiceRequestResponseDto.serviceType( entity.getServiceType() );
        registerServiceRequestResponseDto.status( entity.getStatus() );
        registerServiceRequestResponseDto.updatedAt( entity.getUpdatedAt() );
        registerServiceRequestResponseDto.vehicleBrand( entity.getVehicleBrand() );
        registerServiceRequestResponseDto.vehicleColor( entity.getVehicleColor() );
        registerServiceRequestResponseDto.vehicleType( entity.getVehicleType() );

        registerServiceRequestResponseDto.imageUrls( mapImages(entity) );

        return registerServiceRequestResponseDto.build();
    }

    private Long entityUserId(RegisterServiceRequest registerServiceRequest) {
        if ( registerServiceRequest == null ) {
            return null;
        }
        User user = registerServiceRequest.getUser();
        if ( user == null ) {
            return null;
        }
        Long id = user.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String entityUserEmail(RegisterServiceRequest registerServiceRequest) {
        if ( registerServiceRequest == null ) {
            return null;
        }
        User user = registerServiceRequest.getUser();
        if ( user == null ) {
            return null;
        }
        String email = user.getEmail();
        if ( email == null ) {
            return null;
        }
        return email;
    }
}
