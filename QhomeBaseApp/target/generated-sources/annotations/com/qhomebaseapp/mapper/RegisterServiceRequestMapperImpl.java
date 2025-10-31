package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestDto;
import com.qhomebaseapp.model.RegisterServiceRequest;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-31T21:19:50+0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251023-0518, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class RegisterServiceRequestMapperImpl implements RegisterServiceRequestMapper {

    @Override
    public RegisterServiceRequest toEntity(RegisterServiceRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        RegisterServiceRequest.RegisterServiceRequestBuilder registerServiceRequest = RegisterServiceRequest.builder();

        registerServiceRequest.licensePlate( dto.getLicensePlate() );
        registerServiceRequest.note( dto.getNote() );
        registerServiceRequest.serviceType( dto.getServiceType() );
        registerServiceRequest.vehicleBrand( dto.getVehicleBrand() );
        registerServiceRequest.vehicleColor( dto.getVehicleColor() );
        registerServiceRequest.vehicleType( dto.getVehicleType() );

        return registerServiceRequest.build();
    }
}
