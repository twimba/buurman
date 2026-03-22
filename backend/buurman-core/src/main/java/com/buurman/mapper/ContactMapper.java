package com.buurman.mapper;

import java.util.List;
import java.util.Optional;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.buurman.domain.Contact;
import com.buurman.domain.ContactTag;
import com.buurman.dto.request.CreateContactRequest;
import com.buurman.dto.request.UpdateContactRequest;
import com.buurman.dto.response.ContactListItemResponse;
import com.buurman.dto.response.ContactResponse;
import com.buurman.dto.response.ContactSummary;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface ContactMapper {

  @Mapping(target = "activeProperties", ignore = true)
  @Mapping(target = "mainPhotoUrl", ignore = true)
  @Mapping(target = "mainPhotoThumbnailUrl", ignore = true)
  @Mapping(target = "tags", source = "tags")
  @Mapping(target = "firstName", expression = "java(contact.getFirstName())")
  @Mapping(target = "lastName", expression = "java(contact.getLastName())")
  ContactResponse toResponse(Contact contact);

  @Mapping(target = "firstName", expression = "java(contact.getFirstName())")
  @Mapping(target = "lastName", expression = "java(contact.getLastName())")
  ContactSummary toSummary(Contact contact);

  default ContactListItemResponse toListItem(
      Contact contact,
      int activeContractCount,
      List<ContactTag> tags,
      Optional<String> mainPhotoThumbnailUrl) {
    return new ContactListItemResponse(
        contact.getIdentifier().orElseThrow(),
        contact.getContactType(),
        contact.getDisplayName(),
        contact.getFirstName(),
        contact.getLastName(),
        contact.getEmail(),
        contact.getPhone(),
        contact.getCompanyName(),
        mainPhotoThumbnailUrl,
        List.copyOf(tags),
        activeContractCount,
        Optional.of(contact.getDataRetentionStatus().name()),
        contact.getCreatedAt(),
        Optional.ofNullable(contact.getUpdatedAt()));
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "displayName", ignore = true)
  @Mapping(target = "dataRetentionStatus", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  Contact toEntity(CreateContactRequest request);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "identifier", ignore = true)
  @Mapping(target = "teamId", ignore = true)
  @Mapping(target = "displayName", ignore = true)
  @Mapping(target = "dataRetentionStatus", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  void updateEntity(@MappingTarget Contact contact, UpdateContactRequest request);
}
