package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestResponseDto;
import com.qhomebaseapp.model.RegisterServiceRequest;
import com.qhomebaseapp.model.User;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-10-07T20:11:59+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
public class RegisterServiceRequestResponseMapperImpl implements RegisterServiceRequestResponseMapper {

    @Override
    public RegisterServiceRequestResponseDto toDto(RegisterServiceRequest entity) {
        if ( entity == null ) {
            return null;
        }

        RegisterServiceRequestResponseDto.RegisterServiceRequestResponseDtoBuilder registerServiceRequestResponseDto = RegisterServiceRequestResponseDto.builder();

        registerServiceRequestResponseDto.userId( entityUserId( entity ) );
        registerServiceRequestResponseDto.userEmail( entityUserEmail( entity ) );
        registerServiceRequestResponseDto.id( entity.getId() );
        registerServiceRequestResponseDto.serviceType( entity.getServiceType() );
        registerServiceRequestResponseDto.note( entity.getNote() );
        registerServiceRequestResponseDto.status( entity.getStatus() );
        registerServiceRequestResponseDto.createdAt( entity.getCreatedAt() );
        registerServiceRequestResponseDto.updatedAt( entity.getUpdatedAt() );

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
