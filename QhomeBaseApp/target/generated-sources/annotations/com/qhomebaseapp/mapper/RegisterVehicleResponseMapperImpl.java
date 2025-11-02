package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.registervehicle.RegisterVehicleResponseDto;
import com.qhomebaseapp.model.RegisterVehicleRequest;
import com.qhomebaseapp.model.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-02T10:38:38+0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251023-0518, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class RegisterVehicleResponseMapperImpl implements RegisterVehicleResponseMapper {

    @Override
    public RegisterVehicleResponseDto toDto(RegisterVehicleRequest entity) {
        if ( entity == null ) {
            return null;
        }

        RegisterVehicleResponseDto.RegisterVehicleResponseDtoBuilder registerVehicleResponseDto = RegisterVehicleResponseDto.builder();

        registerVehicleResponseDto.userId( entityUserId( entity ) );
        registerVehicleResponseDto.userEmail( entityUserEmail( entity ) );
        registerVehicleResponseDto.createdAt( entity.getCreatedAt() );
        registerVehicleResponseDto.id( entity.getId() );
        registerVehicleResponseDto.licensePlate( entity.getLicensePlate() );
        registerVehicleResponseDto.note( entity.getNote() );
        registerVehicleResponseDto.paymentDate( entity.getPaymentDate() );
        registerVehicleResponseDto.paymentGateway( entity.getPaymentGateway() );
        registerVehicleResponseDto.paymentStatus( entity.getPaymentStatus() );
        registerVehicleResponseDto.serviceType( entity.getServiceType() );
        registerVehicleResponseDto.status( entity.getStatus() );
        registerVehicleResponseDto.updatedAt( entity.getUpdatedAt() );
        registerVehicleResponseDto.vehicleBrand( entity.getVehicleBrand() );
        registerVehicleResponseDto.vehicleColor( entity.getVehicleColor() );
        registerVehicleResponseDto.vehicleType( entity.getVehicleType() );
        registerVehicleResponseDto.vnpayTransactionRef( entity.getVnpayTransactionRef() );

        registerVehicleResponseDto.imageUrls( mapImages(entity) );

        return registerVehicleResponseDto.build();
    }

    private Long entityUserId(RegisterVehicleRequest registerVehicleRequest) {
        if ( registerVehicleRequest == null ) {
            return null;
        }
        User user = registerVehicleRequest.getUser();
        if ( user == null ) {
            return null;
        }
        Long id = user.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String entityUserEmail(RegisterVehicleRequest registerVehicleRequest) {
        if ( registerVehicleRequest == null ) {
            return null;
        }
        User user = registerVehicleRequest.getUser();
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
