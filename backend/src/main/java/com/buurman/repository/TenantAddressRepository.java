package com.buurman.repository;

import com.buurman.domain.TenantAddress;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.TENANT_ADDRESSES;

@Repository
public class TenantAddressRepository {

    private final DSLContext dsl;

    public TenantAddressRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public TenantAddress save(TenantAddress address) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        if (address.getId() == null) {
            // Insert
            UUID id = UUID.randomUUID();
            LocalDateTime createdAt = address.getCreatedAt() != null
                ? LocalDateTime.ofInstant(address.getCreatedAt(), ZoneOffset.UTC)
                : now;
            LocalDateTime updatedAt = address.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(address.getUpdatedAt(), ZoneOffset.UTC)
                : now;

            dsl.insertInto(TENANT_ADDRESSES)
                    .set(TENANT_ADDRESSES.ID, id)
                    .set(TENANT_ADDRESSES.TENANT_ID, address.getTenantId())
                    .set(TENANT_ADDRESSES.TEAM_ID, address.getTeamId())
                    .set(TENANT_ADDRESSES.STREET, address.getStreet())
                    .set(TENANT_ADDRESSES.CITY, address.getCity())
                    .set(TENANT_ADDRESSES.POSTAL_CODE, address.getPostalCode())
                    .set(TENANT_ADDRESSES.COUNTRY, address.getCountry())
                    .set(TENANT_ADDRESSES.ADDRESS_TYPE, address.getAddressType().name())
                    .set(TENANT_ADDRESSES.STATUS, address.getStatus().name())
                    .set(TENANT_ADDRESSES.LATITUDE, address.getLatitude() != null ? BigDecimal.valueOf(address.getLatitude()) : null)
                    .set(TENANT_ADDRESSES.LONGITUDE, address.getLongitude() != null ? BigDecimal.valueOf(address.getLongitude()) : null)
                    .set(TENANT_ADDRESSES.CREATED_AT, createdAt)
                    .set(TENANT_ADDRESSES.UPDATED_AT, updatedAt)
                    .set(TENANT_ADDRESSES.CREATED_BY, address.getCreatedBy())
                    .set(TENANT_ADDRESSES.UPDATED_BY, address.getUpdatedBy())
                    .execute();
            address.setId(id);
        } else {
            // Update
            LocalDateTime updatedAt = address.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(address.getUpdatedAt(), ZoneOffset.UTC)
                : now;

            dsl.update(TENANT_ADDRESSES)
                    .set(TENANT_ADDRESSES.STREET, address.getStreet())
                    .set(TENANT_ADDRESSES.CITY, address.getCity())
                    .set(TENANT_ADDRESSES.POSTAL_CODE, address.getPostalCode())
                    .set(TENANT_ADDRESSES.COUNTRY, address.getCountry())
                    .set(TENANT_ADDRESSES.ADDRESS_TYPE, address.getAddressType().name())
                    .set(TENANT_ADDRESSES.STATUS, address.getStatus().name())
                    .set(TENANT_ADDRESSES.LATITUDE, address.getLatitude() != null ? BigDecimal.valueOf(address.getLatitude()) : null)
                    .set(TENANT_ADDRESSES.LONGITUDE, address.getLongitude() != null ? BigDecimal.valueOf(address.getLongitude()) : null)
                    .set(TENANT_ADDRESSES.UPDATED_AT, updatedAt)
                    .set(TENANT_ADDRESSES.UPDATED_BY, address.getUpdatedBy())
                    .where(TENANT_ADDRESSES.ID.eq(address.getId())
                            .and(TENANT_ADDRESSES.TEAM_ID.eq(address.getTeamId())))
                    .execute();
        }
        return address;
    }

    public List<TenantAddress> findByTenantId(UUID tenantId, UUID teamId) {
        return dsl.selectFrom(TENANT_ADDRESSES)
                .where(TENANT_ADDRESSES.TENANT_ID.eq(tenantId)
                        .and(TENANT_ADDRESSES.TEAM_ID.eq(teamId))
                        .and(TENANT_ADDRESSES.DELETED_AT.isNull()))
                .fetchInto(TenantAddress.class);
    }

    public Optional<TenantAddress> findByIdAndTeamId(UUID id, UUID teamId) {
        return dsl.selectFrom(TENANT_ADDRESSES)
                .where(TENANT_ADDRESSES.ID.eq(id)
                        .and(TENANT_ADDRESSES.TEAM_ID.eq(teamId))
                        .and(TENANT_ADDRESSES.DELETED_AT.isNull()))
                .fetchOptionalInto(TenantAddress.class);
    }

    public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(TENANT_ADDRESSES)
                .set(TENANT_ADDRESSES.DELETED_AT, now)
                .where(TENANT_ADDRESSES.ID.eq(id).and(TENANT_ADDRESSES.TEAM_ID.eq(teamId)))
                .execute();
    }
}
