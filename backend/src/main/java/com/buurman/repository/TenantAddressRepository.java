package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.TENANT_ADDRESSES;
import static com.buurman.util.SidGenerator.newTenantAddressId;
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

import com.buurman.domain.Sid;
import com.buurman.domain.TenantAddress;
import com.buurman.domain.TenantAddress.AddressStatus;
import com.buurman.domain.TenantAddress.AddressType;
import com.buurman.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TenantAddressRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public TenantAddress save(TenantAddress address) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (address.getId() == null) {
      // Insert
      UUID id = UUID.randomUUID();
      Sid identifier = newTenantAddressId();
      LocalDateTime createdAt = now;
      LocalDateTime updatedAt = now;

      dsl.insertInto(TENANT_ADDRESSES)
          .set(TENANT_ADDRESSES.ID, id)
          .set(TENANT_ADDRESSES.IDENTIFIER, identifier)
          .set(TENANT_ADDRESSES.TENANT_ID, address.getTenantId())
          .set(TENANT_ADDRESSES.TEAM_ID, address.getTeamId())
          .set(TENANT_ADDRESSES.STREET, address.getStreet())
          .set(TENANT_ADDRESSES.CITY, address.getCity())
          .set(TENANT_ADDRESSES.POSTAL_CODE, address.getPostalCode())
          .set(TENANT_ADDRESSES.COUNTRY, address.getCountry())
          .set(TENANT_ADDRESSES.ADDRESS_TYPE, address.getAddressType().name())
          .set(TENANT_ADDRESSES.STATUS, address.getStatus().name())
          .set(
              TENANT_ADDRESSES.LATITUDE,
              address.getLatitude().map(BigDecimal::valueOf).orElse(null))
          .set(
              TENANT_ADDRESSES.LONGITUDE,
              address.getLongitude().map(BigDecimal::valueOf).orElse(null))
          .set(TENANT_ADDRESSES.GEOCODE_ACCURACY, address.getGeocodeAccuracy().orElse(null))
          .set(TENANT_ADDRESSES.CREATED_AT, createdAt)
          .set(TENANT_ADDRESSES.UPDATED_AT, updatedAt)
          .set(TENANT_ADDRESSES.CREATED_BY, address.getCreatedBy())
          .set(TENANT_ADDRESSES.UPDATED_BY, address.getUpdatedBy())
          .execute();
      address.setId(id);
      address.setIdentifier(java.util.Optional.of(identifier));
    } else {
      // Update
      LocalDateTime updatedAt = now;

      dsl.update(TENANT_ADDRESSES)
          .set(TENANT_ADDRESSES.STREET, address.getStreet())
          .set(TENANT_ADDRESSES.CITY, address.getCity())
          .set(TENANT_ADDRESSES.POSTAL_CODE, address.getPostalCode())
          .set(TENANT_ADDRESSES.COUNTRY, address.getCountry())
          .set(TENANT_ADDRESSES.ADDRESS_TYPE, address.getAddressType().name())
          .set(TENANT_ADDRESSES.STATUS, address.getStatus().name())
          .set(
              TENANT_ADDRESSES.LATITUDE,
              address.getLatitude().map(BigDecimal::valueOf).orElse(null))
          .set(
              TENANT_ADDRESSES.LONGITUDE,
              address.getLongitude().map(BigDecimal::valueOf).orElse(null))
          .set(TENANT_ADDRESSES.GEOCODE_ACCURACY, address.getGeocodeAccuracy().orElse(null))
          .set(TENANT_ADDRESSES.UPDATED_AT, updatedAt)
          .set(TENANT_ADDRESSES.UPDATED_BY, address.getUpdatedBy())
          .where(
              TENANT_ADDRESSES
                  .ID
                  .eq(address.getId())
                  .and(TENANT_ADDRESSES.TEAM_ID.eq(address.getTeamId())))
          .execute();
    }
    return address;
  }

  public List<TenantAddress> findByTenantId(UUID tenantId, UUID teamId) {
    return List.copyOf(
        dsl
            .selectFrom(TENANT_ADDRESSES)
            .where(
                TENANT_ADDRESSES
                    .TENANT_ID
                    .eq(tenantId)
                    .and(TENANT_ADDRESSES.TEAM_ID.eq(teamId))
                    .and(TENANT_ADDRESSES.DELETED_AT.isNull()))
            .fetch()
            .stream()
            .map(this::toDomain)
            .toList());
  }

  public Optional<TenantAddress> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(TENANT_ADDRESSES)
        .where(
            TENANT_ADDRESSES
                .IDENTIFIER
                .eq(identifier)
                .and(TENANT_ADDRESSES.TEAM_ID.eq(teamId))
                .and(TENANT_ADDRESSES.DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public Optional<TenantAddress> findByIdAndTeamId(UUID id, UUID teamId) {
    return dsl.selectFrom(TENANT_ADDRESSES)
        .where(
            TENANT_ADDRESSES
                .ID
                .eq(id)
                .and(TENANT_ADDRESSES.TEAM_ID.eq(teamId))
                .and(TENANT_ADDRESSES.DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public TenantAddress getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Tenant address not found"));
  }

  public TenantAddress getByIdAndTeamId(UUID id, UUID teamId) {
    return findByIdAndTeamId(id, teamId)
        .orElseThrow(() -> new NotFoundException("Tenant address not found"));
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(TENANT_ADDRESSES)
        .set(TENANT_ADDRESSES.DELETED_AT, now)
        .where(TENANT_ADDRESSES.ID.eq(id).and(TENANT_ADDRESSES.TEAM_ID.eq(teamId)))
        .execute();
  }

  private TenantAddress toDomain(Record record) {
    return TenantAddress.builder()
        .id(record.get(TENANT_ADDRESSES.ID))
        .identifier(java.util.Optional.of(record.get(TENANT_ADDRESSES.IDENTIFIER)))
        .tenantId(record.get(TENANT_ADDRESSES.TENANT_ID))
        .teamId(record.get(TENANT_ADDRESSES.TEAM_ID))
        .street(record.get(TENANT_ADDRESSES.STREET))
        .city(record.get(TENANT_ADDRESSES.CITY))
        .postalCode(record.get(TENANT_ADDRESSES.POSTAL_CODE))
        .country(record.get(TENANT_ADDRESSES.COUNTRY))
        .addressType(AddressType.valueOf(record.get(TENANT_ADDRESSES.ADDRESS_TYPE)))
        .status(AddressStatus.valueOf(record.get(TENANT_ADDRESSES.STATUS)))
        .latitude(
            Optional.ofNullable(record.get(TENANT_ADDRESSES.LATITUDE)).map(BigDecimal::doubleValue))
        .longitude(
            Optional.ofNullable(record.get(TENANT_ADDRESSES.LONGITUDE))
                .map(BigDecimal::doubleValue))
        .geocodeAccuracy(Optional.ofNullable(record.get(TENANT_ADDRESSES.GEOCODE_ACCURACY)))
        .createdAt(record.get(TENANT_ADDRESSES.CREATED_AT).toInstant(UTC))
        .updatedAt(record.get(TENANT_ADDRESSES.UPDATED_AT).toInstant(UTC))
        .createdBy(record.get(TENANT_ADDRESSES.CREATED_BY))
        .updatedBy(record.get(TENANT_ADDRESSES.UPDATED_BY))
        .deletedAt(
            Optional.ofNullable(record.get(TENANT_ADDRESSES.DELETED_AT))
                .map(ldt -> ldt.toInstant(UTC)))
        .build();
  }
}
