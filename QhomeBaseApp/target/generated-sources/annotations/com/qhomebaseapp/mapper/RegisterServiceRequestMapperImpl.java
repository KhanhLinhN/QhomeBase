package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestDto;
import com.qhomebaseapp.model.RegisterServiceRequest;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-29T23:15:03+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class RegisterServiceRequestMapperImpl implements RegisterServiceRequestMapper {

    @Override
    public RegisterServiceRequest toEntity(RegisterServiceRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        RegisterServiceRequest.RegisterServiceRequestBuilder registerServiceRequest = RegisterServiceRequest.builder();

        registerServiceRequest.serviceType( dto.getServiceType() );
        registerServiceRequest.note( dto.getNote() );
        registerServiceRequest.vehicleType( dto.getVehicleType() );
        registerServiceRequest.licensePlate( dto.getLicensePlate() );
        registerServiceRequest.vehicleBrand( dto.getVehicleBrand() );
        registerServiceRequest.vehicleColor( dto.getVehicleColor() );

        return registerServiceRequest.build();
    }
}
