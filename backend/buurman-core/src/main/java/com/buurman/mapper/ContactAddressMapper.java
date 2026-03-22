package com.buurman.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.buurman.domain.ContactAddress;
import com.buurman.dto.request.CreateContactAddressRequest;
import com.buurman.dto.request.UpdateContactAddressRequest;
import com.buurman.dto.response.ContactAddressResponse;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface ContactAddressMapper {

  ContactAddressResponse toResponse(ContactAddress address);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "contactId", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  ContactAddress toEntity(CreateContactAddressRequest request);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "contactId", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  void updateEntity(@MappingTarget ContactAddress address, UpdateContactAddressRequest request);
}
