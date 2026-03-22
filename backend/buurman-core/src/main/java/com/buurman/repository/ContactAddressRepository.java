package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.CONTACT_ADDRESSES;
import static com.buurman.util.SidGenerator.newContactAddressId;
import static java.time.ZoneOffset.UTC;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import com.buurman.domain.ContactAddress;
import com.buurman.domain.ContactAddress.AddressStatus;
import com.buurman.domain.ContactAddress.AddressType;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ContactAddressRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public ContactAddress save(ContactAddress address) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (address.getId() == null) {
      // Insert
      UUID id = UUID.randomUUID();
      Sid identifier = newContactAddressId();

      dsl.insertInto(CONTACT_ADDRESSES)
          .set(CONTACT_ADDRESSES.ID, id)
          .set(CONTACT_ADDRESSES.IDENTIFIER, identifier)
          .set(CONTACT_ADDRESSES.CONTACT_ID, address.getContactId())
          .set(CONTACT_ADDRESSES.TEAM_ID, address.getTeamId())
          .set(CONTACT_ADDRESSES.STREET, address.getStreet())
          .set(CONTACT_ADDRESSES.CITY, address.getCity())
          .set(CONTACT_ADDRESSES.POSTAL_CODE, address.getPostalCode())
          .set(CONTACT_ADDRESSES.COUNTRY_CODE, address.getCountryCode())
          .set(CONTACT_ADDRESSES.ADDRESS_TYPE, address.getAddressType().name())
          .set(CONTACT_ADDRESSES.STATUS, address.getStatus().name())
          .set(
              CONTACT_ADDRESSES.LATITUDE,
              address.getLatitude().map(BigDecimal::valueOf).orElse(null))
          .set(
              CONTACT_ADDRESSES.LONGITUDE,
              address.getLongitude().map(BigDecimal::valueOf).orElse(null))
          .set(CONTACT_ADDRESSES.GEOCODE_ACCURACY, address.getGeocodeAccuracy().orElse(null))
          .set(CONTACT_ADDRESSES.CREATED_AT, now)
          .set(CONTACT_ADDRESSES.UPDATED_AT, now)
          .set(CONTACT_ADDRESSES.CREATED_BY, address.getCreatedBy())
          .set(CONTACT_ADDRESSES.UPDATED_BY, address.getUpdatedBy())
          .execute();
      address.setId(id);
      address.setIdentifier(java.util.Optional.of(identifier));
    } else {
      // Update — FIX: team_id included in WHERE clause
      dsl.update(CONTACT_ADDRESSES)
          .set(CONTACT_ADDRESSES.STREET, address.getStreet())
          .set(CONTACT_ADDRESSES.CITY, address.getCity())
          .set(CONTACT_ADDRESSES.POSTAL_CODE, address.getPostalCode())
          .set(CONTACT_ADDRESSES.COUNTRY_CODE, address.getCountryCode())
          .set(CONTACT_ADDRESSES.ADDRESS_TYPE, address.getAddressType().name())
          .set(CONTACT_ADDRESSES.STATUS, address.getStatus().name())
          .set(
              CONTACT_ADDRESSES.LATITUDE,
              address.getLatitude().map(BigDecimal::valueOf).orElse(null))
          .set(
              CONTACT_ADDRESSES.LONGITUDE,
              address.getLongitude().map(BigDecimal::valueOf).orElse(null))
          .set(CONTACT_ADDRESSES.GEOCODE_ACCURACY, address.getGeocodeAccuracy().orElse(null))
          .set(CONTACT_ADDRESSES.UPDATED_AT, now)
          .set(CONTACT_ADDRESSES.UPDATED_BY, address.getUpdatedBy())
          .where(
              CONTACT_ADDRESSES
                  .ID
                  .eq(address.getId())
                  .and(CONTACT_ADDRESSES.TEAM_ID.eq(address.getTeamId())))
          .execute();
    }
    return address;
  }

  public List<ContactAddress> findByContactId(UUID contactId, UUID teamId) {
    return List.copyOf(
        dsl
            .selectFrom(CONTACT_ADDRESSES)
            .where(
                CONTACT_ADDRESSES
                    .CONTACT_ID
                    .eq(contactId)
                    .and(CONTACT_ADDRESSES.TEAM_ID.eq(teamId))
                    .and(CONTACT_ADDRESSES.DELETED_AT.isNull()))
            .fetch()
            .stream()
            .map(this::toDomain)
            .toList());
  }

  public Optional<ContactAddress> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(CONTACT_ADDRESSES)
        .where(
            CONTACT_ADDRESSES
                .IDENTIFIER
                .eq(identifier)
                .and(CONTACT_ADDRESSES.TEAM_ID.eq(teamId))
                .and(CONTACT_ADDRESSES.DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public Optional<ContactAddress> findByIdAndTeamId(UUID id, UUID teamId) {
    return dsl.selectFrom(CONTACT_ADDRESSES)
        .where(
            CONTACT_ADDRESSES
                .ID
                .eq(id)
                .and(CONTACT_ADDRESSES.TEAM_ID.eq(teamId))
                .and(CONTACT_ADDRESSES.DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public ContactAddress getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Contact address not found"));
  }

  public ContactAddress getByIdAndTeamId(UUID id, UUID teamId) {
    return findByIdAndTeamId(id, teamId)
        .orElseThrow(() -> new NotFoundException("Contact address not found"));
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(CONTACT_ADDRESSES)
        .set(CONTACT_ADDRESSES.DELETED_AT, now)
        .where(CONTACT_ADDRESSES.ID.eq(id).and(CONTACT_ADDRESSES.TEAM_ID.eq(teamId)))
        .execute();
  }

  public void hardDeleteByContactId(UUID contactId, UUID teamId) {
    dsl.deleteFrom(CONTACT_ADDRESSES)
        .where(CONTACT_ADDRESSES.CONTACT_ID.eq(contactId).and(CONTACT_ADDRESSES.TEAM_ID.eq(teamId)))
        .execute();
  }

  private ContactAddress toDomain(Record record) {
    return ContactAddress.builder()
        .id(record.get(CONTACT_ADDRESSES.ID))
        .identifier(java.util.Optional.of(record.get(CONTACT_ADDRESSES.IDENTIFIER)))
        .contactId(record.get(CONTACT_ADDRESSES.CONTACT_ID))
        .teamId(record.get(CONTACT_ADDRESSES.TEAM_ID))
        .street(record.get(CONTACT_ADDRESSES.STREET))
        .city(record.get(CONTACT_ADDRESSES.CITY))
        .postalCode(record.get(CONTACT_ADDRESSES.POSTAL_CODE))
        .countryCode(record.get(CONTACT_ADDRESSES.COUNTRY_CODE))
        .addressType(AddressType.valueOf(record.get(CONTACT_ADDRESSES.ADDRESS_TYPE)))
        .status(AddressStatus.valueOf(record.get(CONTACT_ADDRESSES.STATUS)))
        .latitude(
            Optional.ofNullable(record.get(CONTACT_ADDRESSES.LATITUDE))
                .map(BigDecimal::doubleValue))
        .longitude(
            Optional.ofNullable(record.get(CONTACT_ADDRESSES.LONGITUDE))
                .map(BigDecimal::doubleValue))
        .geocodeAccuracy(Optional.ofNullable(record.get(CONTACT_ADDRESSES.GEOCODE_ACCURACY)))
        .createdAt(record.get(CONTACT_ADDRESSES.CREATED_AT).toInstant(UTC))
        .updatedAt(record.get(CONTACT_ADDRESSES.UPDATED_AT).toInstant(UTC))
        .createdBy(record.get(CONTACT_ADDRESSES.CREATED_BY))
        .updatedBy(record.get(CONTACT_ADDRESSES.UPDATED_BY))
        .deletedAt(
            Optional.ofNullable(record.get(CONTACT_ADDRESSES.DELETED_AT))
                .map(ldt -> ldt.toInstant(UTC)))
        .build();
  }
}
