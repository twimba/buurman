package com.buurman.mapper;

import com.buurman.domain.Tenant;
import com.buurman.dto.request.CreateTenantRequest;
import com.buurman.dto.request.UpdateTenantRequest;
import com.buurman.dto.response.TenantResponse;
import com.buurman.dto.response.TenantSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TenantMapper {

    @Mapping(target = "currentProperty", ignore = true)
    TenantResponse toResponse(Tenant tenant);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "identifier", source = "identifier")
    @Mapping(target = "firstName", source = "firstName")
    @Mapping(target = "lastName", source = "lastName")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
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
