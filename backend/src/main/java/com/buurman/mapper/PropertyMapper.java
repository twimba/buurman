package com.buurman.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.buurman.domain.Property;
import com.buurman.dto.request.CreatePropertyRequest;
import com.buurman.dto.request.UpdatePropertyRequest;
import com.buurman.dto.response.PropertyResponse;
import com.buurman.dto.response.PropertySummary;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface PropertyMapper {

  @Mapping(target = "mainPhotoUrl", ignore = true)
  @Mapping(target = "mainPhotoThumbnailUrl", ignore = true)
  @Mapping(target = "outdoorAreas", ignore = true)
  @Mapping(target = "amenities", ignore = true)
  @Mapping(target = "residentialDetails", ignore = true)
  @Mapping(target = "commercialDetails", ignore = true)
  @Mapping(target = "industrialDetails", ignore = true)
  @Mapping(target = "agriculturalDetails", ignore = true)
  PropertyResponse toResponse(Property property);

  PropertySummary toSummary(Property property);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  Property toEntity(CreatePropertyRequest request);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "propertyCategory", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  void updateEntity(@MappingTarget Property property, UpdatePropertyRequest request);
}
