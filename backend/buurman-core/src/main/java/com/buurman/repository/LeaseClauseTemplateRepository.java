package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.LEASE_CLAUSE_TEMPLATES;
import static com.buurman.util.SidGenerator.newLeaseClauseTemplateId;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.LeaseClauseTemplate;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.LeaseClauseTemplateRecordMapper;

import lombok.RequiredArgsConstructor;

/**
 * Global reference data — no {@code team_id} filtering anywhere in this repository. Lease clause
 * templates are shared across all teams; only per-contract overrides (see {@link
 * ContractLeaseClauseRepository}) are team-scoped.
 */
@Repository
@RequiredArgsConstructor
public class LeaseClauseTemplateRepository {

  private final DSLContext dsl;
  private final LeaseClauseTemplateRecordMapper mapper;
  private final Clock clock;

  public List<LeaseClauseTemplate> findByCountryCode(String countryCode) {
    return List.copyOf(
        dsl.selectFrom(LEASE_CLAUSE_TEMPLATES)
            .where(
                LEASE_CLAUSE_TEMPLATES
                    .COUNTRY_CODE
                    .eq(countryCode)
                    .and(LEASE_CLAUSE_TEMPLATES.DELETED_AT.isNull()))
            .orderBy(LEASE_CLAUSE_TEMPLATES.SORT_ORDER.asc())
            .fetch()
            .map(mapper::toDomain));
  }

  public List<LeaseClauseTemplate> findByCountryAndKind(String countryCode, LeaseKind kind) {
    return List.copyOf(
        dsl.selectFrom(LEASE_CLAUSE_TEMPLATES)
            .where(
                LEASE_CLAUSE_TEMPLATES
                    .COUNTRY_CODE
                    .eq(countryCode)
                    .and(LEASE_CLAUSE_TEMPLATES.LEASE_KIND.eq(kind.name()))
                    .and(LEASE_CLAUSE_TEMPLATES.DELETED_AT.isNull()))
            .orderBy(LEASE_CLAUSE_TEMPLATES.SORT_ORDER.asc())
            .fetch()
            .map(mapper::toDomain));
  }

  public Optional<LeaseClauseTemplate> findByIdentifier(Sid identifier) {
    return dsl.selectFrom(LEASE_CLAUSE_TEMPLATES)
        .where(
            LEASE_CLAUSE_TEMPLATES
                .IDENTIFIER
                .eq(identifier)
                .and(LEASE_CLAUSE_TEMPLATES.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public LeaseClauseTemplate getByIdentifier(Sid identifier) {
    return findByIdentifier(identifier)
        .orElseThrow(() -> new NotFoundException("Lease clause template not found"));
  }

  public LeaseClauseTemplate save(LeaseClauseTemplate template) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (template.getId() == null) {
      UUID newId = UUID.randomUUID();
      Sid identifier = newLeaseClauseTemplateId();

      dsl.insertInto(LEASE_CLAUSE_TEMPLATES)
          .set(LEASE_CLAUSE_TEMPLATES.ID, newId)
          .set(LEASE_CLAUSE_TEMPLATES.IDENTIFIER, identifier)
          .set(LEASE_CLAUSE_TEMPLATES.COUNTRY_CODE, template.getCountryCode())
          .set(LEASE_CLAUSE_TEMPLATES.LEASE_KIND, template.getLeaseKind().name())
          .set(LEASE_CLAUSE_TEMPLATES.CLAUSE_KEY, template.getClauseKey())
          .set(LEASE_CLAUSE_TEMPLATES.TITLE_I18N_KEY, template.getTitleI18nKey())
          .set(LEASE_CLAUSE_TEMPLATES.BODY_I18N_KEY, template.getBodyI18nKey())
          .set(LEASE_CLAUSE_TEMPLATES.DEFAULT_INCLUDED, template.isDefaultIncluded())
          .set(LEASE_CLAUSE_TEMPLATES.OPTIONAL, template.isOptional())
          .set(LEASE_CLAUSE_TEMPLATES.PINNED, template.isPinned())
          .set(LEASE_CLAUSE_TEMPLATES.SORT_ORDER, template.getSortOrder())
          .set(LEASE_CLAUSE_TEMPLATES.VERSION, template.getVersion())
          .set(LEASE_CLAUSE_TEMPLATES.CREATED_AT, now)
          .set(LEASE_CLAUSE_TEMPLATES.UPDATED_AT, now)
          .set(LEASE_CLAUSE_TEMPLATES.CREATED_BY, template.getCreatedBy())
          .set(LEASE_CLAUSE_TEMPLATES.UPDATED_BY, template.getUpdatedBy())
          .execute();

      template.setId(newId);
      template.setIdentifier(Optional.of(identifier));
    } else {
      dsl.update(LEASE_CLAUSE_TEMPLATES)
          .set(LEASE_CLAUSE_TEMPLATES.TITLE_I18N_KEY, template.getTitleI18nKey())
          .set(LEASE_CLAUSE_TEMPLATES.BODY_I18N_KEY, template.getBodyI18nKey())
          .set(LEASE_CLAUSE_TEMPLATES.DEFAULT_INCLUDED, template.isDefaultIncluded())
          .set(LEASE_CLAUSE_TEMPLATES.OPTIONAL, template.isOptional())
          .set(LEASE_CLAUSE_TEMPLATES.PINNED, template.isPinned())
          .set(LEASE_CLAUSE_TEMPLATES.SORT_ORDER, template.getSortOrder())
          .set(LEASE_CLAUSE_TEMPLATES.VERSION, template.getVersion())
          .set(LEASE_CLAUSE_TEMPLATES.UPDATED_AT, now)
          .set(LEASE_CLAUSE_TEMPLATES.UPDATED_BY, template.getUpdatedBy())
          .where(LEASE_CLAUSE_TEMPLATES.ID.eq(template.getId()))
          .execute();
    }
    return template;
  }

  public void softDeleteByIdentifier(Sid identifier) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(LEASE_CLAUSE_TEMPLATES)
        .set(LEASE_CLAUSE_TEMPLATES.DELETED_AT, now)
        .where(LEASE_CLAUSE_TEMPLATES.IDENTIFIER.eq(identifier))
        .execute();
  }
}
