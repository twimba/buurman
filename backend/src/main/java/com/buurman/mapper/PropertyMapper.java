package com.buurman.mapper;

import com.buurman.domain.Property;
import com.buurman.dto.request.CreatePropertyRequest;
import com.buurman.dto.request.UpdatePropertyRequest;
import com.buurman.dto.response.PropertyResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PropertyMapper {

    @Mapping(target = "mainPhotoUrl", ignore = true)
    PropertyResponse toResponse(Property property);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "identifier", ignore = true)
    @Mapping(target = "teamId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Property toEntity(CreatePropertyRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "identifier", ignore = true)
    @Mapping(target = "teamId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(@MappingTarget Property property, UpdatePropertyRequest request);
}
