package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.registervehicle.RegisterVehicleRequestDto;
import com.qhomebaseapp.model.RegisterVehicleRequest;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-02T10:38:38+0700",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.44.0.v20251023-0518, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class RegisterVehicleMapperImpl implements RegisterVehicleMapper {

    @Override
    public RegisterVehicleRequest toEntity(RegisterVehicleRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        RegisterVehicleRequest.RegisterVehicleRequestBuilder registerVehicleRequest = RegisterVehicleRequest.builder();

        registerVehicleRequest.licensePlate( dto.getLicensePlate() );
        registerVehicleRequest.note( dto.getNote() );
        registerVehicleRequest.serviceType( dto.getServiceType() );
        registerVehicleRequest.vehicleBrand( dto.getVehicleBrand() );
        registerVehicleRequest.vehicleColor( dto.getVehicleColor() );
        registerVehicleRequest.vehicleType( dto.getVehicleType() );

        return registerVehicleRequest.build();
    }
}
