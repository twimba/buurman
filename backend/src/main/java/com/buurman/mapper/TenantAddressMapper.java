package com.buurman.mapper;

import com.buurman.domain.TenantAddress;
import com.buurman.dto.request.CreateTenantAddressRequest;
import com.buurman.dto.request.UpdateTenantAddressRequest;
import com.buurman.dto.response.TenantAddressResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TenantAddressMapper {

    TenantAddressResponse toResponse(TenantAddress address);

    TenantAddress toEntity(CreateTenantAddressRequest request);

    void updateEntity(@MappingTarget TenantAddress address, UpdateTenantAddressRequest request);
}
