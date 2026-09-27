package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.buurman.domain.Contact;
import com.buurman.jooq.generated.tables.records.ContactsRecord;

@Mapper(componentModel = "spring")
public interface ContactRecordMapper {

  @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
  @Mapping(target = "deletedAt", expression = "java(toOptionalInstant(record.getDeletedAt()))")
  @Mapping(
      target = "contactType",
      expression = "java(com.buurman.domain.ContactType.valueOf(record.getContactType()))")
  @Mapping(
      target = "preferredLanguage",
      expression = "java(java.util.Optional.ofNullable(record.getPreferredLanguage()))")
  @Mapping(
      target = "firstName",
      expression = "java(java.util.Optional.ofNullable(record.getFirstName()))")
  @Mapping(
      target = "lastName",
      expression = "java(java.util.Optional.ofNullable(record.getLastName()))")
  @Mapping(
      target = "companyName",
      expression = "java(java.util.Optional.ofNullable(record.getCompanyName()))")
  @Mapping(
      target = "tradeName",
      expression = "java(java.util.Optional.ofNullable(record.getTradeName()))")
  @Mapping(
      target = "industry",
      expression = "java(java.util.Optional.ofNullable(record.getIndustry()))")
  @Mapping(target = "email", expression = "java(java.util.Optional.ofNullable(record.getEmail()))")
  @Mapping(
      target = "invoiceEmail",
      expression = "java(java.util.Optional.ofNullable(record.getInvoiceEmail()))")
  @Mapping(target = "phone", expression = "java(java.util.Optional.ofNullable(record.getPhone()))")
  @Mapping(
      target = "website",
      expression = "java(java.util.Optional.ofNullable(record.getWebsite()))")
  @Mapping(
      target = "taxNumber",
      expression = "java(java.util.Optional.ofNullable(record.getTaxNumber()))")
  @Mapping(
      target = "idNumber",
      expression = "java(java.util.Optional.ofNullable(record.getIdNumber()))")
  @Mapping(
      target = "dateOfBirth",
      expression = "java(java.util.Optional.ofNullable(record.getDateOfBirth()))")
  @Mapping(
      target = "idExpiryDate",
      expression = "java(java.util.Optional.ofNullable(record.getIdExpiryDate()))")
  @Mapping(target = "notes", expression = "java(java.util.Optional.ofNullable(record.getNotes()))")
  @Mapping(
      target = "dataRetentionStatus",
      expression =
          "java(com.buurman.domain.DataRetentionStatus.valueOf(record.getDataRetentionStatus()))")
  @Mapping(
      target = "identifier",
      expression = "java(java.util.Optional.of(record.getIdentifier()))")
  @Mapping(target = "tags", expression = "java(java.util.List.of())")
  Contact toDomain(ContactsRecord record);

  List<Contact> toDomainList(List<ContactsRecord> records);

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }

  default Optional<Instant> toOptionalInstant(@Nullable LocalDateTime localDateTime) {
    return Optional.ofNullable(localDateTime).map(dt -> dt.toInstant(UTC));
  }

  default @Nullable LocalDateTime toLocalDateTime(@Nullable Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, UTC);
  }
}
