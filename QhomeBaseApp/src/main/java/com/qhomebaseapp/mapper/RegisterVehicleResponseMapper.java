package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.registervehicle.RegisterVehicleResponseDto;
import com.qhomebaseapp.model.RegisterVehicleImage;
import com.qhomebaseapp.model.RegisterVehicleRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RegisterVehicleResponseMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "imageUrls", expression = "java(mapImages(entity))")
    RegisterVehicleResponseDto toDto(RegisterVehicleRequest entity);
    default List<String> mapImages(RegisterVehicleRequest entity) {
        if (entity == null || entity.getImages() == null) return java.util.Collections.emptyList();
        return entity.getImages().stream()
                .map(RegisterVehicleImage::getImageUrl)
                .collect(Collectors.toList());
    }
}

