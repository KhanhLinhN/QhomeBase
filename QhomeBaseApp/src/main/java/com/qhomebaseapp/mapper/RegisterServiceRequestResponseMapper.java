package com.qhomebaseapp.mapper;

import com.qhomebaseapp.dto.registrationservice.RegisterServiceRequestResponseDto;
import com.qhomebaseapp.model.RegisterServiceImage;
import com.qhomebaseapp.model.RegisterServiceRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RegisterServiceRequestResponseMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "imageUrls", expression = "java(mapImages(entity))")
    RegisterServiceRequestResponseDto toDto(RegisterServiceRequest entity);
    default List<String> mapImages(RegisterServiceRequest entity) {
        if (entity == null || entity.getImages() == null) return java.util.Collections.emptyList();
        return entity.getImages().stream()
                .map(RegisterServiceImage::getImageUrl)
                .collect(Collectors.toList());
    }
}
