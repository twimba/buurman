package com.buurman.repository;

import com.buurman.domain.Tenant;
import com.buurman.dto.request.PageRequest;
import com.buurman.mapper.TenantRecordMapper;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import static org.jooq.impl.DSL.lower;
import org.springframework.stereotype.Repository;

import com.buurman.jooq.generated.tables.records.TenantsRecord;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    public Optional<Tenant> findByIdentifierAndTeamId(String identifier, UUID teamId) {
        return dsl.selectFrom(TENANTS)
                .where(TENANTS.IDENTIFIER.eq(identifier)
                        .and(TENANTS.TEAM_ID.eq(teamId))
                        .and(TENANTS.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
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
                        .and(lower(TENANTS.FIRST_NAME).like(searchPattern)
                                .or(lower(TENANTS.LAST_NAME).like(searchPattern))
                                .or(lower(TENANTS.EMAIL).like(searchPattern))
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
                    .set(TENANTS.FIRST_NAME, tenant.getFirstName())
                    .set(TENANTS.LAST_NAME, tenant.getLastName())
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
                    .set(TENANTS.FIRST_NAME, tenant.getFirstName())
                    .set(TENANTS.LAST_NAME, tenant.getLastName())
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

    public PaginatedResult<Tenant> findAllByTeamIdPaginated(UUID teamId, String search, PageRequest pageRequest) {
        Condition condition = TENANTS.TEAM_ID.eq(teamId).and(TENANTS.DELETED_AT.isNull());
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            condition = condition.and(
                lower(TENANTS.FIRST_NAME).like(pattern)
                    .or(lower(TENANTS.LAST_NAME).like(pattern))
                    .or(lower(TENANTS.EMAIL).like(pattern))
                    .or(TENANTS.PHONE.like(pattern))
            );
        }
        Map<String, Field<?>> sortableFields = Map.of(
            "createdAt", TENANTS.CREATED_AT,
            "firstName", TENANTS.FIRST_NAME,
            "lastName", TENANTS.LAST_NAME,
            "email", TENANTS.EMAIL
        );
        return PaginationHelper.paginate(dsl, TENANTS, condition, sortableFields, TENANTS.CREATED_AT, pageRequest, r -> mapper.toDomain((TenantsRecord) r));
    }

    public List<Tenant> findByIdsAndTeamId(Collection<UUID> ids, UUID teamId) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return dsl.selectFrom(TENANTS)
                .where(TENANTS.ID.in(ids)
                        .and(TENANTS.TEAM_ID.eq(teamId))
                        .and(TENANTS.DELETED_AT.isNull()))
                .fetch()
                .map(mapper::toDomain);
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
