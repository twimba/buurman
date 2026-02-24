package com.buurman.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.buurman.domain.Tenant;
import com.buurman.dto.request.CreateTenantRequest;
import com.buurman.dto.request.UpdateTenantRequest;
import com.buurman.dto.response.TenantResponse;
import com.buurman.dto.response.TenantSummary;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface TenantMapper {

  @Mapping(target = "activeProperties", ignore = true)
  @Mapping(target = "mainPhotoUrl", ignore = true)
  @Mapping(target = "mainPhotoThumbnailUrl", ignore = true)
  @Mapping(target = "lastName", expression = "java(tenant.getLastName().orElse(null))")
  TenantResponse toResponse(Tenant tenant);

  @Mapping(target = "lastName", expression = "java(tenant.getLastName().orElse(null))")
  TenantSummary toSummary(Tenant tenant);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "currentPropertyId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  Tenant toEntity(CreateTenantRequest request);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "currentPropertyId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  void updateEntity(@MappingTarget Tenant tenant, UpdateTenantRequest request);
}
