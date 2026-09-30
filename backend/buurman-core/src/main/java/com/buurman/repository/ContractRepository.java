package com.buurman.repository;

import static com.buurman.domain.Contract.ContractStatus.ACTIVE;
import static com.buurman.jooq.generated.Tables.CONTACTS;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.CONTRACT_PARTIES;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.lower;
import static org.jooq.impl.DSL.min;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractIncomeEntry;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Sid;
import com.buurman.domain.UnitActiveTenancy;
import com.buurman.domain.metadata.CountryMetadataSerializer;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.ContractRecordMapper;
import com.buurman.util.MoneyAmount;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ContractRepository {

  private final DSLContext dsl;
  private final ContractRecordMapper mapper;
  private final CountryMetadataSerializer countryMetadataSerializer;
  private final Clock clock;

  /** Status names of {@link Contract.ContractStatus#IN_FORCE}, for {@code STATUS.in(...)}. */
  private static final List<String> IN_FORCE_STATUS_NAMES =
      Contract.ContractStatus.IN_FORCE.stream().map(Enum::name).sorted().toList();

  // Fields for columns added in V012 (not yet in JOOQ generated code)
  private static final Field<String> COUNTRY_CODE =
      org.jooq.impl.DSL.field("country_code", String.class);
  private static final Field<JSONB> COUNTRY_METADATA =
      org.jooq.impl.DSL.field("country_metadata", JSONB.class);
  // Field for column added in V048
  private static final Field<String[]> DOCUMENT_LANGUAGES =
      org.jooq.impl.DSL.field("document_languages", String[].class);

  public Optional<Contract> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(CONTRACTS)
        .where(
            CONTRACTS
                .IDENTIFIER
                .eq(identifier)
                .and(CONTRACTS.TEAM_ID.eq(teamId))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public Contract getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Contract not found"));
  }

  public Optional<Contract> findByIdAndTeamId(UUID id, UUID teamId) {
    return dsl.selectFrom(CONTRACTS)
        .where(
            CONTRACTS
                .ID
                .eq(id)
                .and(CONTRACTS.TEAM_ID.eq(teamId))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public Contract getByIdAndTeamId(UUID id, UUID teamId) {
    return findByIdAndTeamId(id, teamId)
        .orElseThrow(() -> new NotFoundException("Contract not found"));
  }

  public List<Contract> findByIdsAndTeamId(Collection<UUID> ids, UUID teamId) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return dsl
        .selectFrom(CONTRACTS)
        .where(
            CONTRACTS
                .ID
                .in(ids)
                .and(CONTRACTS.TEAM_ID.eq(teamId))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public List<Contract> findAllByTeamId(UUID teamId) {
    return dsl
        .selectFrom(CONTRACTS)
        .where(CONTRACTS.TEAM_ID.eq(teamId).and(CONTRACTS.DELETED_AT.isNull()))
        .orderBy(CONTRACTS.CREATED_AT.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public List<Contract> findByPropertyId(UUID propertyId, UUID teamId) {
    return dsl
        .selectFrom(CONTRACTS)
        .where(
            CONTRACTS
                .PROPERTY_ID
                .eq(propertyId)
                .and(CONTRACTS.TEAM_ID.eq(teamId))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .orderBy(CONTRACTS.START_DATE.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public List<Contract> findByUnitId(UUID unitId, UUID teamId) {
    return dsl
        .selectFrom(CONTRACTS)
        .where(
            CONTRACTS
                .UNIT_ID
                .eq(unitId)
                .and(CONTRACTS.TEAM_ID.eq(teamId))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .orderBy(CONTRACTS.START_DATE.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  /**
   * Whether any contract (active or historical) still references this unit — used to guard unit
   * deletion, since {@code GET /contracts/{id}} resolves the contract's unit by id and would
   * otherwise 404 once the unit is soft-deleted, permanently hiding the tenancy's history.
   */
  public boolean existsByUnitId(UUID unitId, UUID teamId) {
    return dsl.fetchExists(
        dsl.selectOne()
            .from(CONTRACTS)
            .where(
                CONTRACTS
                    .UNIT_ID
                    .eq(unitId)
                    .and(CONTRACTS.TEAM_ID.eq(teamId))
                    .and(CONTRACTS.DELETED_AT.isNull())));
  }

  public List<Contract> findByContactIdViaParties(UUID contactId, UUID teamId) {
    var CONTRACT_PARTIES = org.jooq.impl.DSL.table("contract_parties");
    var CP_CONTRACT_ID = org.jooq.impl.DSL.field("contract_parties.contract_id", UUID.class);
    var CP_CONTACT_ID = org.jooq.impl.DSL.field("contract_parties.contact_id", UUID.class);
    var CP_TEAM_ID = org.jooq.impl.DSL.field("contract_parties.team_id", UUID.class);
    var CP_DELETED_AT = org.jooq.impl.DSL.field("contract_parties.deleted_at", LocalDateTime.class);

    return dsl
        .selectFrom(CONTRACTS)
        .whereExists(
            dsl.selectOne()
                .from(CONTRACT_PARTIES)
                .where(
                    CP_CONTRACT_ID
                        .eq(CONTRACTS.ID)
                        .and(CP_CONTACT_ID.eq(contactId))
                        .and(CP_TEAM_ID.eq(teamId))
                        .and(CP_DELETED_AT.isNull())))
        .and(CONTRACTS.TEAM_ID.eq(teamId))
        .and(CONTRACTS.DELETED_AT.isNull())
        .orderBy(CONTRACTS.START_DATE.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public List<Contract> findByStatus(Contract.ContractStatus status, UUID teamId) {
    return dsl
        .selectFrom(CONTRACTS)
        .where(
            CONTRACTS
                .STATUS
                .eq(status.name())
                .and(CONTRACTS.TEAM_ID.eq(teamId))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .orderBy(CONTRACTS.START_DATE.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  /**
   * The contract currently in force on the unit — {@code ACTIVE} or {@code NOTICE_GIVEN} (a tenant
   * under notice still occupies the unit until the effective end date). At most one row is expected
   * ({@code uq_contracts_one_active_per_unit}, added in V072, plus the in-force guard in {@code
   * ContractService} that refuses to activate a second contract), but a legacy duplicate predating
   * that constraint is still possible. {@code limit(1)} with a deterministic {@code orderBy} makes
   * that degrade to "pick the oldest" instead of throwing {@link
   * org.jooq.exception.TooManyRowsException} (which {@code fetchOptional()} does on >1 row and
   * which was previously an unhandled 500).
   */
  public Optional<Contract> findActiveByUnitId(UUID unitId, UUID teamId) {
    return dsl.selectFrom(CONTRACTS)
        .where(
            CONTRACTS
                .UNIT_ID
                .eq(unitId)
                .and(CONTRACTS.TEAM_ID.eq(teamId))
                .and(CONTRACTS.STATUS.in(IN_FORCE_STATUS_NAMES))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .orderBy(CONTRACTS.CREATED_AT.asc(), CONTRACTS.ID.asc())
        .limit(1)
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public Contract save(Contract contract) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (contract.getId() == null) {
      // Insert
      UUID id = UUID.randomUUID();

      dsl.insertInto(CONTRACTS)
          .set(CONTRACTS.ID, id)
          .set(CONTRACTS.IDENTIFIER, contract.getIdentifier().orElseThrow())
          .set(CONTRACTS.TEAM_ID, contract.getTeamId())
          .set(CONTRACTS.PROPERTY_ID, contract.getPropertyId())
          .set(CONTRACTS.UNIT_ID, contract.getUnitId())
          .set(CONTRACTS.CONTRACT_TYPE, contract.getContractType().name())
          .set(CONTRACTS.START_DATE, contract.getStartDate())
          .set(CONTRACTS.END_DATE, contract.getEndDate().orElse(null))
          .set(CONTRACTS.SIGNED_DATE, contract.getSignedDate().orElse(null))
          .set(CONTRACTS.RENT_AMOUNT, contract.getRentAmount().value())
          .set(CONTRACTS.RENT_AMOUNT_CURRENCY, contract.getRentAmount().currency())
          .set(
              CONTRACTS.DEPOSIT_AMOUNT,
              contract.getDepositAmount().map(MoneyAmount::value).orElse(null))
          .set(
              CONTRACTS.DEPOSIT_AMOUNT_CURRENCY,
              contract.getDepositAmount().map(MoneyAmount::currency).orElse(null))
          .set(
              CONTRACTS.SECURITY_DEPOSIT,
              contract.getSecurityDeposit().map(MoneyAmount::value).orElse(null))
          .set(
              CONTRACTS.SECURITY_DEPOSIT_CURRENCY,
              contract.getSecurityDeposit().map(MoneyAmount::currency).orElse(null))
          .set(CONTRACTS.PAYMENT_FREQUENCY, contract.getPaymentFrequency().name())
          .set(CONTRACTS.PAYMENT_DUE_DAY, contract.getPaymentDueDay().orElse(null))
          .set(CONTRACTS.TERMINATION_NOTICE_DAYS, contract.getTerminationNoticeDays())
          .set(CONTRACTS.LATE_FEE_PERCENTAGE, contract.getLateFeePercentage().orElse(null))
          .set(CONTRACTS.LATE_FEE_ENABLED, contract.getLateFeeEnabled())
          .set(CONTRACTS.LATE_FEE_GRACE_DAYS, contract.getLateFeeGraceDays())
          .set(CONTRACTS.FORMAL_NOTICE_DAYS, contract.getFormalNoticeDays().orElse(null))
          .set(CONTRACTS.STATUS, contract.getStatus().name())
          .set(CONTRACTS.TERMS_AND_CONDITIONS, contract.getTermsAndConditions().orElse(null))
          .set(CONTRACTS.NOTES, contract.getNotes().orElse(null))
          .set(COUNTRY_CODE, contract.getCountryCode().orElse(null))
          .set(
              COUNTRY_METADATA,
              contract
                  .getCountryMetadata()
                  .map(m -> JSONB.jsonb(countryMetadataSerializer.serialize(m)))
                  .orElse(null))
          .set(CONTRACTS.RENEWAL_MODE, contract.getRenewalMode().name())
          .set(CONTRACTS.RENEWAL_TERM_MONTHS, contract.getRenewalTermMonths().orElse(null))
          .set(CONTRACTS.MAX_RENEWALS, contract.getMaxRenewals().orElse(null))
          .set(CONTRACTS.LANDLORD_NOTICE_DAYS, contract.getLandlordNoticeDays())
          .set(CONTRACTS.TENANT_NOTICE_DAYS, contract.getTenantNoticeDays())
          .set(CONTRACTS.REQUIRES_TENANT_CONFIRMATION, contract.getRequiresTenantConfirmation())
          .set(CONTRACTS.TENANT_REMINDERS_ENABLED, contract.getTenantRemindersEnabled())
          .set(CONTRACTS.REMINDERS_PAUSED_UNTIL, contract.getRemindersPausedUntil().orElse(null))
          .set(CONTRACTS.RENT_ADJUSTMENT_TYPE, contract.getRentAdjustmentType().name())
          .set(CONTRACTS.RENT_ADJUSTMENT_VALUE, contract.getRentAdjustmentValue().orElse(null))
          .set(CONTRACTS.LANDLORD_TYPE, contract.getLandlordType().map(Enum::name).orElse(null))
          .set(CONTRACTS.REGION_CODE, contract.getRegionCode().orElse(null))
          .set(DOCUMENT_LANGUAGES, contract.getDocumentLanguages().toArray(new String[0]))
          .set(CONTRACTS.CREATED_AT, now)
          .set(CONTRACTS.UPDATED_AT, now)
          .set(CONTRACTS.CREATED_BY, contract.getCreatedBy())
          .set(CONTRACTS.UPDATED_BY, contract.getUpdatedBy())
          .execute();

      contract.setId(id);
      contract.setCreatedAt(now.toInstant(UTC));
      contract.setUpdatedAt(now.toInstant(UTC));
    } else {
      // Update

      var query =
          dsl.update(CONTRACTS)
              .set(CONTRACTS.PROPERTY_ID, contract.getPropertyId())
              .set(CONTRACTS.UNIT_ID, contract.getUnitId())
              .set(CONTRACTS.CONTRACT_TYPE, contract.getContractType().name())
              .set(CONTRACTS.START_DATE, contract.getStartDate())
              .set(CONTRACTS.END_DATE, contract.getEndDate().orElse(null))
              .set(CONTRACTS.SIGNED_DATE, contract.getSignedDate().orElse(null))
              .set(CONTRACTS.RENT_AMOUNT, contract.getRentAmount().value())
              .set(CONTRACTS.RENT_AMOUNT_CURRENCY, contract.getRentAmount().currency())
              .set(
                  CONTRACTS.DEPOSIT_AMOUNT,
                  contract.getDepositAmount().map(MoneyAmount::value).orElse(null))
              .set(
                  CONTRACTS.DEPOSIT_AMOUNT_CURRENCY,
                  contract.getDepositAmount().map(MoneyAmount::currency).orElse(null))
              .set(
                  CONTRACTS.SECURITY_DEPOSIT,
                  contract.getSecurityDeposit().map(MoneyAmount::value).orElse(null))
              .set(
                  CONTRACTS.SECURITY_DEPOSIT_CURRENCY,
                  contract.getSecurityDeposit().map(MoneyAmount::currency).orElse(null))
              .set(CONTRACTS.PAYMENT_FREQUENCY, contract.getPaymentFrequency().name())
              .set(CONTRACTS.PAYMENT_DUE_DAY, contract.getPaymentDueDay().orElse(null))
              .set(CONTRACTS.TERMINATION_NOTICE_DAYS, contract.getTerminationNoticeDays())
              .set(CONTRACTS.LATE_FEE_PERCENTAGE, contract.getLateFeePercentage().orElse(null))
              .set(CONTRACTS.LATE_FEE_ENABLED, contract.getLateFeeEnabled())
              .set(CONTRACTS.LATE_FEE_GRACE_DAYS, contract.getLateFeeGraceDays())
              .set(CONTRACTS.FORMAL_NOTICE_DAYS, contract.getFormalNoticeDays().orElse(null))
              .set(CONTRACTS.STATUS, contract.getStatus().name())
              .set(CONTRACTS.TERMS_AND_CONDITIONS, contract.getTermsAndConditions().orElse(null))
              .set(CONTRACTS.NOTES, contract.getNotes().orElse(null))
              .set(CONTRACTS.RENEWAL_MODE, contract.getRenewalMode().name())
              .set(CONTRACTS.RENEWAL_TERM_MONTHS, contract.getRenewalTermMonths().orElse(null))
              .set(CONTRACTS.MAX_RENEWALS, contract.getMaxRenewals().orElse(null))
              .set(CONTRACTS.LANDLORD_NOTICE_DAYS, contract.getLandlordNoticeDays())
              .set(CONTRACTS.TENANT_NOTICE_DAYS, contract.getTenantNoticeDays())
              .set(CONTRACTS.REQUIRES_TENANT_CONFIRMATION, contract.getRequiresTenantConfirmation())
              .set(CONTRACTS.TENANT_REMINDERS_ENABLED, contract.getTenantRemindersEnabled())
              .set(
                  CONTRACTS.REMINDERS_PAUSED_UNTIL, contract.getRemindersPausedUntil().orElse(null))
              .set(CONTRACTS.RENT_ADJUSTMENT_TYPE, contract.getRentAdjustmentType().name())
              .set(CONTRACTS.RENT_ADJUSTMENT_VALUE, contract.getRentAdjustmentValue().orElse(null))
              .set(CONTRACTS.LANDLORD_TYPE, contract.getLandlordType().map(Enum::name).orElse(null))
              .set(CONTRACTS.REGION_CODE, contract.getRegionCode().orElse(null))
              .set(DOCUMENT_LANGUAGES, contract.getDocumentLanguages().toArray(new String[0]));

      // Only update country_code and country_metadata while contract is DRAFT (locked after
      // activation)
      if (contract.getStatus() == Contract.ContractStatus.DRAFT) {
        query =
            query
                .set(COUNTRY_CODE, contract.getCountryCode().orElse(null))
                .set(
                    COUNTRY_METADATA,
                    contract
                        .getCountryMetadata()
                        .map(m -> JSONB.jsonb(countryMetadataSerializer.serialize(m)))
                        .orElse(null));
      }

      query
          .set(CONTRACTS.UPDATED_AT, now)
          .set(CONTRACTS.UPDATED_BY, contract.getUpdatedBy())
          .where(CONTRACTS.ID.eq(contract.getId()).and(CONTRACTS.TEAM_ID.eq(contract.getTeamId())))
          .execute();

      contract.setUpdatedAt(now.toInstant(UTC));
    }

    return contract;
  }

  public PaginatedResult<Contract> findAllByTeamIdPaginated(
      UUID teamId,
      @Nullable String status,
      @Nullable UUID propertyId,
      @Nullable UUID contactId,
      @Nullable String search,
      @Nullable Integer endingWithinDays,
      PageRequest pageRequest) {
    Condition condition =
        buildListCondition(teamId, status, propertyId, contactId, search, endingWithinDays);
    Field<LocalDate> effectiveEndDate =
        com.buurman.service.EffectiveEndDateHelper.effectiveEndDate();
    Map<String, Field<?>> sortableFields =
        Map.of(
            "createdAt", CONTRACTS.CREATED_AT,
            "startDate", CONTRACTS.START_DATE,
            "endDate", effectiveEndDate,
            "rentAmount", CONTRACTS.RENT_AMOUNT,
            "status", CONTRACTS.STATUS);
    return PaginationHelper.paginate(
        dsl,
        CONTRACTS,
        condition,
        sortableFields,
        CONTRACTS.CREATED_AT,
        pageRequest,
        r ->
            mapper
                .toDomain(r)
                .orElseThrow(() -> new IllegalStateException("Failed to map contract record")));
  }

  public List<Contract> findAllByTeamId(
      UUID teamId,
      @Nullable String status,
      @Nullable String search,
      @Nullable Integer endingWithinDays) {
    Condition condition = buildListCondition(teamId, status, null, null, search, endingWithinDays);
    return dsl
        .selectFrom(CONTRACTS)
        .where(condition)
        .orderBy(CONTRACTS.CREATED_AT.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  private Condition buildListCondition(
      UUID teamId,
      @Nullable String status,
      @Nullable UUID propertyId,
      @Nullable UUID contactId,
      @Nullable String search,
      @Nullable Integer endingWithinDays) {
    Condition condition = CONTRACTS.TEAM_ID.eq(teamId).and(CONTRACTS.DELETED_AT.isNull());
    if (status != null && !status.isEmpty()) {
      condition = condition.and(CONTRACTS.STATUS.eq(status));
    }
    if (propertyId != null) {
      condition = condition.and(CONTRACTS.PROPERTY_ID.eq(propertyId));
    }
    if (contactId != null) {
      var CP_CONTRACT_ID = org.jooq.impl.DSL.field("contract_parties.contract_id", UUID.class);
      var CP_CONTACT_ID = org.jooq.impl.DSL.field("contract_parties.contact_id", UUID.class);
      var CP_TEAM_ID = org.jooq.impl.DSL.field("contract_parties.team_id", UUID.class);
      var CP_DELETED_AT =
          org.jooq.impl.DSL.field("contract_parties.deleted_at", LocalDateTime.class);
      condition =
          condition.and(
              org.jooq.impl.DSL.exists(
                  dsl.selectOne()
                      .from(org.jooq.impl.DSL.table("contract_parties"))
                      .where(
                          CP_CONTRACT_ID
                              .eq(CONTRACTS.ID)
                              .and(CP_CONTACT_ID.eq(contactId))
                              .and(CP_TEAM_ID.eq(teamId))
                              .and(CP_DELETED_AT.isNull()))));
    }
    if (search != null && !search.trim().isEmpty()) {
      String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
      condition =
          condition.and(
              org.jooq
                  .impl
                  .DSL
                  .exists(
                      dsl.selectOne()
                          .from(CONTRACT_PARTIES)
                          .join(CONTACTS)
                          .on(
                              CONTACTS
                                  .ID
                                  .eq(CONTRACT_PARTIES.CONTACT_ID)
                                  .and(CONTACTS.TEAM_ID.eq(teamId))
                                  .and(CONTACTS.DELETED_AT.isNull()))
                          .where(
                              CONTRACT_PARTIES
                                  .CONTRACT_ID
                                  .eq(CONTRACTS.ID)
                                  .and(CONTRACT_PARTIES.TEAM_ID.eq(teamId))
                                  .and(CONTRACT_PARTIES.DELETED_AT.isNull())
                                  .and(lower(CONTACTS.DISPLAY_NAME).like(pattern))))
                  .or(
                      org.jooq.impl.DSL.exists(
                          dsl.selectOne()
                              .from(PROPERTIES)
                              .where(
                                  PROPERTIES
                                      .ID
                                      .eq(CONTRACTS.PROPERTY_ID)
                                      .and(PROPERTIES.TEAM_ID.eq(teamId))
                                      .and(PROPERTIES.DELETED_AT.isNull())
                                      .and(
                                          lower(PROPERTIES.STREET)
                                              .like(pattern)
                                              .or(lower(PROPERTIES.CITY).like(pattern))))))
                  .or(lower(CONTRACTS.IDENTIFIER.cast(String.class)).like(pattern)));
    }
    if (endingWithinDays != null) {
      LocalDate today = LocalDate.now(clock);
      Field<LocalDate> effectiveEndDateExpr =
          com.buurman.service.EffectiveEndDateHelper.effectiveEndDateExpr();
      condition =
          condition.and(
              effectiveEndDateExpr
                  .isNotNull()
                  .and(effectiveEndDateExpr.between(today, today.plusDays(endingWithinDays))));
    }
    return condition;
  }

  public List<Contract> findActiveByTeamId(UUID teamId) {
    return dsl
        .selectFrom(CONTRACTS)
        .where(
            CONTRACTS
                .TEAM_ID
                .eq(teamId)
                .and(CONTRACTS.STATUS.eq(ACTIVE.name()))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  /**
   * Contracts still in force ({@code ACTIVE} or {@code NOTICE_GIVEN}): the ones that must keep
   * generating rent up to their effective end date. Unlike {@link #findActiveByTeamId}, a contract
   * under notice is included.
   */
  public List<Contract> findInForceByTeamId(UUID teamId) {
    return dsl
        .selectFrom(CONTRACTS)
        .where(
            CONTRACTS
                .TEAM_ID
                .eq(teamId)
                .and(CONTRACTS.STATUS.in(IN_FORCE_STATUS_NAMES))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .orderBy(CONTRACTS.START_DATE.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public List<ContractIncomeEntry> findActiveContractIncomeByTeamId(UUID teamId) {
    return dsl.select(
            CONTRACTS.RENT_AMOUNT, CONTRACTS.RENT_AMOUNT_CURRENCY, CONTRACTS.PAYMENT_FREQUENCY)
        .from(CONTRACTS)
        .where(
            CONTRACTS
                .TEAM_ID
                .eq(teamId)
                .and(CONTRACTS.STATUS.eq("ACTIVE"))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .fetch()
        .map(
            r ->
                new ContractIncomeEntry(
                    r.get(CONTRACTS.RENT_AMOUNT),
                    r.get(CONTRACTS.RENT_AMOUNT_CURRENCY),
                    r.get(CONTRACTS.PAYMENT_FREQUENCY)));
  }

  public List<Contract> findExpiringContracts(UUID teamId, LocalDate beforeDate) {
    Field<LocalDate> effectiveEndDate =
        com.buurman.service.EffectiveEndDateHelper.effectiveEndDateExpr();
    return dsl
        .selectFrom(CONTRACTS)
        .where(
            CONTRACTS
                .TEAM_ID
                .eq(teamId)
                .and(CONTRACTS.STATUS.eq(ACTIVE.name()))
                .and(effectiveEndDate.isNotNull())
                .and(effectiveEndDate.le(beforeDate))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public Optional<LocalDate> findEarliestStartDate(UUID teamId) {
    return Optional.ofNullable(
        dsl.select(min(CONTRACTS.START_DATE))
            .from(CONTRACTS)
            .where(
                CONTRACTS
                    .TEAM_ID
                    .eq(teamId)
                    .and(CONTRACTS.DELETED_AT.isNull())
                    .and(CONTRACTS.START_DATE.isNotNull()))
            .fetchOne(min(CONTRACTS.START_DATE)));
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(CONTRACTS)
        .set(CONTRACTS.DELETED_AT, now)
        .where(CONTRACTS.ID.eq(id).and(CONTRACTS.TEAM_ID.eq(teamId)))
        .execute();
  }

  public int countByTeamId(UUID teamId) {
    return dsl.fetchCount(
        dsl.selectFrom(CONTRACTS)
            .where(CONTRACTS.TEAM_ID.eq(teamId).and(CONTRACTS.DELETED_AT.isNull())));
  }

  /**
   * Contracts occupying the unit: in force ({@code ACTIVE} or {@code NOTICE_GIVEN}, see {@link
   * Contract.ContractStatus#IN_FORCE}) and not soft-deleted.
   */
  public int countActiveByUnitId(UUID unitId, UUID teamId) {
    return dsl.fetchCount(
        dsl.selectFrom(CONTRACTS)
            .where(
                CONTRACTS
                    .UNIT_ID
                    .eq(unitId)
                    .and(CONTRACTS.TEAM_ID.eq(teamId))
                    .and(CONTRACTS.STATUS.in(IN_FORCE_STATUS_NAMES))
                    .and(CONTRACTS.DELETED_AT.isNull())));
  }

  /**
   * In-force ({@code ACTIVE} or {@code NOTICE_GIVEN}) contracts for the given units, paired with
   * the rent charged and the primary tenant's display name (via {@code contract_parties}/{@code
   * contacts}), for assembling the units grid view. {@code tenantName} is {@code null} when no
   * primary-tenant party is recorded.
   */
  public List<UnitActiveTenancy> findActiveTenanciesByUnitIds(
      Collection<UUID> unitIds, UUID teamId) {
    if (unitIds == null || unitIds.isEmpty()) {
      return List.of();
    }
    return dsl.select(
            CONTRACTS.UNIT_ID,
            CONTRACTS.RENT_AMOUNT,
            CONTRACTS.RENT_AMOUNT_CURRENCY,
            CONTACTS.DISPLAY_NAME)
        .from(CONTRACTS)
        .leftJoin(CONTRACT_PARTIES)
        .on(
            CONTRACT_PARTIES
                .CONTRACT_ID
                .eq(CONTRACTS.ID)
                .and(CONTRACT_PARTIES.ROLE.eq(ContractPartyRole.PRIMARY_TENANT.name()))
                .and(CONTRACT_PARTIES.TEAM_ID.eq(teamId))
                .and(CONTRACT_PARTIES.DELETED_AT.isNull()))
        .leftJoin(CONTACTS)
        .on(
            CONTACTS
                .ID
                .eq(CONTRACT_PARTIES.CONTACT_ID)
                .and(CONTACTS.TEAM_ID.eq(teamId))
                .and(CONTACTS.DELETED_AT.isNull()))
        .where(
            CONTRACTS
                .UNIT_ID
                .in(unitIds)
                .and(CONTRACTS.TEAM_ID.eq(teamId))
                .and(CONTRACTS.STATUS.in(IN_FORCE_STATUS_NAMES))
                .and(CONTRACTS.DELETED_AT.isNull()))
        // Deterministic order so that, in the schema-permitted case of two active contracts (or
        // two PRIMARY_TENANT parties) on one unit, the caller's "keep the first" merge picks the
        // most recently started tenancy rather than whatever order the planner returned.
        .orderBy(CONTRACTS.START_DATE.desc())
        .fetch(
            r ->
                new UnitActiveTenancy(
                    r.get(CONTRACTS.UNIT_ID),
                    r.get(CONTRACTS.RENT_AMOUNT),
                    r.get(CONTRACTS.RENT_AMOUNT_CURRENCY),
                    r.get(CONTACTS.DISPLAY_NAME)));
  }
}
