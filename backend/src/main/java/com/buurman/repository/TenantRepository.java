package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.TENANTS;
import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.lower;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Tenant;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.jooq.generated.tables.records.TenantsRecord;
import com.buurman.mapper.TenantRecordMapper;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TenantRepository {

  private final DSLContext dsl;
  private final TenantRecordMapper mapper;
  private final Clock clock;

  public Optional<Tenant> findByIdentifierAndTeamId(String identifier, UUID teamId) {
    return dsl.selectFrom(TENANTS)
        .where(
            TENANTS
                .IDENTIFIER
                .eq(identifier)
                .and(TENANTS.TEAM_ID.eq(teamId))
                .and(TENANTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public Optional<Tenant> findByIdAndTeamId(UUID id, UUID teamId) {
    return dsl.selectFrom(TENANTS)
        .where(TENANTS.ID.eq(id).and(TENANTS.TEAM_ID.eq(teamId)).and(TENANTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public Tenant getByIdentifierAndTeamId(String identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Tenant not found"));
  }

  public Tenant getByIdAndTeamId(UUID id, UUID teamId) {
    return findByIdAndTeamId(id, teamId)
        .orElseThrow(() -> new NotFoundException("Tenant not found"));
  }

  public List<Tenant> findAllByTeamId(UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(TENANTS)
            .where(TENANTS.TEAM_ID.eq(teamId).and(TENANTS.DELETED_AT.isNull()))
            .orderBy(TENANTS.CREATED_AT.desc())
            .fetch()
            .map(mapper::toDomain));
  }

  public List<Tenant> searchByTeamId(UUID teamId, String searchTerm) {
    String searchPattern = "%" + searchTerm.toLowerCase() + "%";
    return List.copyOf(
        dsl.selectFrom(TENANTS)
            .where(
                TENANTS
                    .TEAM_ID
                    .eq(teamId)
                    .and(TENANTS.DELETED_AT.isNull())
                    .and(
                        lower(TENANTS.FIRST_NAME)
                            .like(searchPattern)
                            .or(lower(TENANTS.LAST_NAME).like(searchPattern))
                            .or(lower(TENANTS.EMAIL).like(searchPattern))
                            .or(TENANTS.PHONE.like(searchPattern))))
            .orderBy(TENANTS.CREATED_AT.desc())
            .fetch()
            .map(mapper::toDomain));
  }

  public List<Tenant> findByCurrentPropertyId(UUID propertyId, UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(TENANTS)
            .where(
                TENANTS
                    .CURRENT_PROPERTY_ID
                    .eq(propertyId)
                    .and(TENANTS.TEAM_ID.eq(teamId))
                    .and(TENANTS.DELETED_AT.isNull()))
            .fetch()
            .map(mapper::toDomain));
  }

  public Optional<Tenant> findByEmailAndTeamId(String email, UUID teamId) {
    return dsl.selectFrom(TENANTS)
        .where(
            TENANTS
                .EMAIL
                .eq(email)
                .and(TENANTS.TEAM_ID.eq(teamId))
                .and(TENANTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public Tenant save(Tenant tenant) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (tenant.getId() == null) {
      // INSERT
      UUID newId = UUID.randomUUID();
      LocalDateTime createdAt =
          tenant.getCreatedAt() != null ? LocalDateTime.ofInstant(tenant.getCreatedAt(), UTC) : now;
      LocalDateTime updatedAt =
          tenant.getUpdatedAt() != null ? LocalDateTime.ofInstant(tenant.getUpdatedAt(), UTC) : now;

      dsl.insertInto(TENANTS)
          .set(TENANTS.ID, newId)
          .set(TENANTS.IDENTIFIER, tenant.getIdentifier())
          .set(TENANTS.TEAM_ID, tenant.getTeamId())
          .set(TENANTS.FIRST_NAME, tenant.getFirstName())
          .set(TENANTS.LAST_NAME, tenant.getLastName())
          .set(TENANTS.EMAIL, tenant.getEmail())
          .set(TENANTS.PHONE, tenant.getPhone().orElse(null))
          .set(TENANTS.TAX_NUMBER, tenant.getTaxNumber().orElse(null))
          .set(TENANTS.ID_NUMBER, tenant.getIdNumber().orElse(null))
          .set(TENANTS.ADDITIONAL_INFO, tenant.getAdditionalInfo().orElse(null))
          .set(TENANTS.CURRENT_PROPERTY_ID, tenant.getCurrentPropertyId().orElse(null))
          .set(TENANTS.CREATED_AT, createdAt)
          .set(TENANTS.UPDATED_AT, updatedAt)
          .set(TENANTS.CREATED_BY, tenant.getCreatedBy())
          .set(TENANTS.UPDATED_BY, tenant.getUpdatedBy())
          .execute();

      tenant.setId(newId);
      tenant.setCreatedAt(createdAt.toInstant(UTC));
      tenant.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      // UPDATE
      LocalDateTime updatedAt =
          tenant.getUpdatedAt() != null ? LocalDateTime.ofInstant(tenant.getUpdatedAt(), UTC) : now;

      dsl.update(TENANTS)
          .set(TENANTS.FIRST_NAME, tenant.getFirstName())
          .set(TENANTS.LAST_NAME, tenant.getLastName())
          .set(TENANTS.EMAIL, tenant.getEmail())
          .set(TENANTS.PHONE, tenant.getPhone().orElse(null))
          .set(TENANTS.TAX_NUMBER, tenant.getTaxNumber().orElse(null))
          .set(TENANTS.ID_NUMBER, tenant.getIdNumber().orElse(null))
          .set(TENANTS.ADDITIONAL_INFO, tenant.getAdditionalInfo().orElse(null))
          .set(TENANTS.CURRENT_PROPERTY_ID, tenant.getCurrentPropertyId().orElse(null))
          .set(TENANTS.UPDATED_AT, updatedAt)
          .set(TENANTS.UPDATED_BY, tenant.getUpdatedBy())
          .where(TENANTS.ID.eq(tenant.getId()).and(TENANTS.TEAM_ID.eq(tenant.getTeamId())))
          .execute();

      tenant.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return tenant;
  }

  public PaginatedResult<Tenant> findAllByTeamIdPaginated(
      UUID teamId, @Nullable String search, PageRequest pageRequest) {
    Condition condition = TENANTS.TEAM_ID.eq(teamId).and(TENANTS.DELETED_AT.isNull());
    if (search != null && !search.isBlank()) {
      String pattern = "%" + search.toLowerCase() + "%";
      condition =
          condition.and(
              lower(TENANTS.FIRST_NAME)
                  .like(pattern)
                  .or(lower(TENANTS.LAST_NAME).like(pattern))
                  .or(lower(TENANTS.EMAIL).like(pattern))
                  .or(TENANTS.PHONE.like(pattern)));
    }
    Map<String, Field<?>> sortableFields =
        Map.of(
            "createdAt", TENANTS.CREATED_AT,
            "firstName", TENANTS.FIRST_NAME,
            "lastName", TENANTS.LAST_NAME,
            "email", TENANTS.EMAIL);
    return PaginationHelper.paginate(
        dsl,
        TENANTS,
        condition,
        sortableFields,
        TENANTS.CREATED_AT,
        pageRequest,
        r -> mapper.toDomain((TenantsRecord) r));
  }

  public List<Tenant> findByIdsAndTeamId(Collection<UUID> ids, UUID teamId) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return List.copyOf(
        dsl.selectFrom(TENANTS)
            .where(
                TENANTS.ID.in(ids).and(TENANTS.TEAM_ID.eq(teamId)).and(TENANTS.DELETED_AT.isNull()))
            .fetch()
            .map(mapper::toDomain));
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(TENANTS)
        .set(TENANTS.DELETED_AT, now)
        .where(TENANTS.ID.eq(id).and(TENANTS.TEAM_ID.eq(teamId)))
        .execute();
  }
}
