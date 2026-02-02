package com.buurman.repository;

import com.buurman.domain.Tenant;
import com.buurman.mapper.TenantRecordMapper;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.TENANTS;

@Repository
public class TenantRepository {

    private final DSLContext dsl;
    private final TenantRecordMapper mapper;

    public TenantRepository(DSLContext dsl, TenantRecordMapper mapper) {
        this.dsl = dsl;
        this.mapper = mapper;
    }

    public Optional<Tenant> findByIdAndTeamId(UUID id, UUID teamId) {
        return dsl.selectFrom(TENANTS)
                .where(TENANTS.ID.eq(id)
                        .and(TENANTS.TEAM_ID.eq(teamId))
                        .and(TENANTS.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public List<Tenant> findAllByTeamId(UUID teamId) {
        return dsl.selectFrom(TENANTS)
                .where(TENANTS.TEAM_ID.eq(teamId)
                        .and(TENANTS.DELETED_AT.isNull()))
                .orderBy(TENANTS.CREATED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public List<Tenant> searchByTeamId(UUID teamId, String searchTerm) {
        String searchPattern = "%" + searchTerm.toLowerCase() + "%";
        return dsl.selectFrom(TENANTS)
                .where(TENANTS.TEAM_ID.eq(teamId)
                        .and(TENANTS.DELETED_AT.isNull())
                        .and(TENANTS.NAME.lower().like(searchPattern)
                                .or(TENANTS.EMAIL.lower().like(searchPattern))
                                .or(TENANTS.PHONE.like(searchPattern))))
                .orderBy(TENANTS.CREATED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public List<Tenant> findByCurrentPropertyId(UUID propertyId, UUID teamId) {
        return dsl.selectFrom(TENANTS)
                .where(TENANTS.CURRENT_PROPERTY_ID.eq(propertyId)
                        .and(TENANTS.TEAM_ID.eq(teamId))
                        .and(TENANTS.DELETED_AT.isNull()))
                .fetch()
                .map(mapper::toDomain);
    }

    public Optional<Tenant> findByEmailAndTeamId(String email, UUID teamId) {
        return dsl.selectFrom(TENANTS)
                .where(TENANTS.EMAIL.eq(email)
                        .and(TENANTS.TEAM_ID.eq(teamId))
                        .and(TENANTS.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public Tenant save(Tenant tenant) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        if (tenant.getId() == null) {
            // INSERT
            UUID newId = UUID.randomUUID();
            dsl.insertInto(TENANTS)
                    .set(TENANTS.ID, newId)
                    .set(TENANTS.IDENTIFIER, tenant.getIdentifier())
                    .set(TENANTS.TEAM_ID, tenant.getTeamId())
                    .set(TENANTS.NAME, tenant.getName())
                    .set(TENANTS.EMAIL, tenant.getEmail())
                    .set(TENANTS.PHONE, tenant.getPhone())
                    .set(TENANTS.TAX_NUMBER, tenant.getTaxNumber())
                    .set(TENANTS.ID_NUMBER, tenant.getIdNumber())
                    .set(TENANTS.ADDITIONAL_INFO, tenant.getAdditionalInfo())
                    .set(TENANTS.CURRENT_PROPERTY_ID, tenant.getCurrentPropertyId())
                    .set(TENANTS.CREATED_AT, now)
                    .set(TENANTS.UPDATED_AT, now)
                    .set(TENANTS.CREATED_BY, tenant.getCreatedBy())
                    .set(TENANTS.UPDATED_BY, tenant.getUpdatedBy())
                    .execute();

            tenant.setId(newId);
            tenant.setCreatedAt(now.toInstant(ZoneOffset.UTC));
            tenant.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        } else {
            // UPDATE
            dsl.update(TENANTS)
                    .set(TENANTS.NAME, tenant.getName())
                    .set(TENANTS.EMAIL, tenant.getEmail())
                    .set(TENANTS.PHONE, tenant.getPhone())
                    .set(TENANTS.TAX_NUMBER, tenant.getTaxNumber())
                    .set(TENANTS.ID_NUMBER, tenant.getIdNumber())
                    .set(TENANTS.ADDITIONAL_INFO, tenant.getAdditionalInfo())
                    .set(TENANTS.CURRENT_PROPERTY_ID, tenant.getCurrentPropertyId())
                    .set(TENANTS.UPDATED_AT, now)
                    .set(TENANTS.UPDATED_BY, tenant.getUpdatedBy())
                    .where(TENANTS.ID.eq(tenant.getId())
                            .and(TENANTS.TEAM_ID.eq(tenant.getTeamId())))
                    .execute();

            tenant.setUpdatedAt(now.toInstant(ZoneOffset.UTC));
        }

        return tenant;
    }

    public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(TENANTS)
                .set(TENANTS.DELETED_AT, now)
                .where(TENANTS.ID.eq(id)
                        .and(TENANTS.TEAM_ID.eq(teamId)))
                .execute();
    }
}
