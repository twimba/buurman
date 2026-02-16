package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.TENANT_ADDRESSES;
import static com.buurman.util.UlidGenerator.newTenantAddressId;
import static java.time.ZoneOffset.UTC;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.TenantAddress;
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
      String identifier = newTenantAddressId().value();
      LocalDateTime createdAt =
          address.getCreatedAt() != null
              ? LocalDateTime.ofInstant(address.getCreatedAt(), UTC)
              : now;
      LocalDateTime updatedAt =
          address.getUpdatedAt() != null
              ? LocalDateTime.ofInstant(address.getUpdatedAt(), UTC)
              : now;

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
              address.getLatitude() != null ? BigDecimal.valueOf(address.getLatitude()) : null)
          .set(
              TENANT_ADDRESSES.LONGITUDE,
              address.getLongitude() != null ? BigDecimal.valueOf(address.getLongitude()) : null)
          .set(TENANT_ADDRESSES.CREATED_AT, createdAt)
          .set(TENANT_ADDRESSES.UPDATED_AT, updatedAt)
          .set(TENANT_ADDRESSES.CREATED_BY, address.getCreatedBy())
          .set(TENANT_ADDRESSES.UPDATED_BY, address.getUpdatedBy())
          .execute();
      address.setId(id);
      address.setIdentifier(identifier);
    } else {
      // Update
      LocalDateTime updatedAt =
          address.getUpdatedAt() != null
              ? LocalDateTime.ofInstant(address.getUpdatedAt(), UTC)
              : now;

      dsl.update(TENANT_ADDRESSES)
          .set(TENANT_ADDRESSES.STREET, address.getStreet())
          .set(TENANT_ADDRESSES.CITY, address.getCity())
          .set(TENANT_ADDRESSES.POSTAL_CODE, address.getPostalCode())
          .set(TENANT_ADDRESSES.COUNTRY, address.getCountry())
          .set(TENANT_ADDRESSES.ADDRESS_TYPE, address.getAddressType().name())
          .set(TENANT_ADDRESSES.STATUS, address.getStatus().name())
          .set(
              TENANT_ADDRESSES.LATITUDE,
              address.getLatitude() != null ? BigDecimal.valueOf(address.getLatitude()) : null)
          .set(
              TENANT_ADDRESSES.LONGITUDE,
              address.getLongitude() != null ? BigDecimal.valueOf(address.getLongitude()) : null)
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
    return dsl.selectFrom(TENANT_ADDRESSES)
        .where(
            TENANT_ADDRESSES
                .TENANT_ID
                .eq(tenantId)
                .and(TENANT_ADDRESSES.TEAM_ID.eq(teamId))
                .and(TENANT_ADDRESSES.DELETED_AT.isNull()))
        .fetchInto(TenantAddress.class);
  }

  public Optional<TenantAddress> findByIdentifierAndTeamId(String identifier, UUID teamId) {
    return dsl.selectFrom(TENANT_ADDRESSES)
        .where(
            TENANT_ADDRESSES
                .IDENTIFIER
                .eq(identifier)
                .and(TENANT_ADDRESSES.TEAM_ID.eq(teamId))
                .and(TENANT_ADDRESSES.DELETED_AT.isNull()))
        .fetchOptionalInto(TenantAddress.class);
  }

  public Optional<TenantAddress> findByIdAndTeamId(UUID id, UUID teamId) {
    return dsl.selectFrom(TENANT_ADDRESSES)
        .where(
            TENANT_ADDRESSES
                .ID
                .eq(id)
                .and(TENANT_ADDRESSES.TEAM_ID.eq(teamId))
                .and(TENANT_ADDRESSES.DELETED_AT.isNull()))
        .fetchOptionalInto(TenantAddress.class);
  }

  public TenantAddress getByIdentifierAndTeamId(String identifier, UUID teamId) {
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
}
