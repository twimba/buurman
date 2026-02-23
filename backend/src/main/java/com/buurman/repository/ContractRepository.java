package com.buurman.repository;

import static com.buurman.domain.Contract.ContractStatus.ACTIVE;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.min;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Contract;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.jooq.generated.tables.records.ContractsRecord;
import com.buurman.mapper.ContractRecordMapper;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ContractRepository {

  private final DSLContext dsl;
  private final ContractRecordMapper mapper;
  private final Clock clock;

  public Optional<Contract> findByIdentifierAndTeamId(String identifier, UUID teamId) {
    return dsl.selectFrom(CONTRACTS)
        .where(
            CONTRACTS
                .IDENTIFIER
                .eq(identifier)
                .and(CONTRACTS.TEAM_ID.eq(teamId))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public Contract getByIdentifierAndTeamId(String identifier, UUID teamId) {
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
        .map(mapper::toDomain);
  }

  public Contract getByIdAndTeamId(UUID id, UUID teamId) {
    return findByIdAndTeamId(id, teamId)
        .orElseThrow(() -> new NotFoundException("Contract not found"));
  }

  public List<Contract> findByIdsAndTeamId(Collection<UUID> ids, UUID teamId) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return dsl.selectFrom(CONTRACTS)
        .where(
            CONTRACTS
                .ID
                .in(ids)
                .and(CONTRACTS.TEAM_ID.eq(teamId))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .fetch()
        .map(mapper::toDomain);
  }

  public List<Contract> findAllByTeamId(UUID teamId) {
    return dsl.selectFrom(CONTRACTS)
        .where(CONTRACTS.TEAM_ID.eq(teamId).and(CONTRACTS.DELETED_AT.isNull()))
        .orderBy(CONTRACTS.CREATED_AT.desc())
        .fetch()
        .map(mapper::toDomain);
  }

  public List<Contract> findByPropertyId(UUID propertyId, UUID teamId) {
    return dsl.selectFrom(CONTRACTS)
        .where(
            CONTRACTS
                .PROPERTY_ID
                .eq(propertyId)
                .and(CONTRACTS.TEAM_ID.eq(teamId))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .orderBy(CONTRACTS.START_DATE.desc())
        .fetch()
        .map(mapper::toDomain);
  }

  public List<Contract> findByTenantIdViaParties(UUID tenantId, UUID teamId) {
    var CONTRACT_PARTIES = org.jooq.impl.DSL.table("contract_parties");
    var CP_CONTRACT_ID = org.jooq.impl.DSL.field("contract_parties.contract_id", UUID.class);
    var CP_TENANT_ID = org.jooq.impl.DSL.field("contract_parties.tenant_id", UUID.class);
    var CP_TEAM_ID = org.jooq.impl.DSL.field("contract_parties.team_id", UUID.class);
    var CP_DELETED_AT = org.jooq.impl.DSL.field("contract_parties.deleted_at", LocalDateTime.class);

    return dsl.selectFrom(CONTRACTS)
        .whereExists(
            dsl.selectOne()
                .from(CONTRACT_PARTIES)
                .where(
                    CP_CONTRACT_ID
                        .eq(CONTRACTS.ID)
                        .and(CP_TENANT_ID.eq(tenantId))
                        .and(CP_TEAM_ID.eq(teamId))
                        .and(CP_DELETED_AT.isNull())))
        .and(CONTRACTS.TEAM_ID.eq(teamId))
        .and(CONTRACTS.DELETED_AT.isNull())
        .orderBy(CONTRACTS.START_DATE.desc())
        .fetch()
        .map(mapper::toDomain);
  }

  public List<Contract> findByStatus(Contract.ContractStatus status, UUID teamId) {
    return dsl.selectFrom(CONTRACTS)
        .where(
            CONTRACTS
                .STATUS
                .eq(status.name())
                .and(CONTRACTS.TEAM_ID.eq(teamId))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .orderBy(CONTRACTS.START_DATE.desc())
        .fetch()
        .map(mapper::toDomain);
  }

  public Optional<Contract> findActiveContractByPropertyId(UUID propertyId, UUID teamId) {
    return dsl.selectFrom(CONTRACTS)
        .where(
            CONTRACTS
                .PROPERTY_ID
                .eq(propertyId)
                .and(CONTRACTS.TEAM_ID.eq(teamId))
                .and(CONTRACTS.STATUS.eq(ACTIVE.name()))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public Contract save(Contract contract) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (contract.getId() == null) {
      // Insert
      UUID id = UUID.randomUUID();
      LocalDateTime createdAt =
          contract.getCreatedAt() != null
              ? LocalDateTime.ofInstant(contract.getCreatedAt(), UTC)
              : now;
      LocalDateTime updatedAt =
          contract.getUpdatedAt() != null
              ? LocalDateTime.ofInstant(contract.getUpdatedAt(), UTC)
              : now;

      dsl.insertInto(CONTRACTS)
          .set(CONTRACTS.ID, id)
          .set(CONTRACTS.IDENTIFIER, contract.getIdentifier())
          .set(CONTRACTS.TEAM_ID, contract.getTeamId())
          .set(CONTRACTS.PROPERTY_ID, contract.getPropertyId())
          .set(CONTRACTS.CONTRACT_TYPE, contract.getContractType().name())
          .set(CONTRACTS.START_DATE, contract.getStartDate())
          .set(CONTRACTS.END_DATE, contract.getEndDate())
          .set(CONTRACTS.SIGNED_DATE, contract.getSignedDate())
          .set(
              CONTRACTS.RENT_AMOUNT,
              CurrencyUtils.toMinorUnits(
                  contract.getRentAmount(), contract.getRentAmountCurrency()))
          .set(CONTRACTS.RENT_AMOUNT_CURRENCY, contract.getRentAmountCurrency())
          .set(
              CONTRACTS.DEPOSIT_AMOUNT,
              CurrencyUtils.toMinorUnitsOrNull(
                  contract.getDepositAmount(), contract.getDepositAmountCurrency()))
          .set(CONTRACTS.DEPOSIT_AMOUNT_CURRENCY, contract.getDepositAmountCurrency())
          .set(
              CONTRACTS.SECURITY_DEPOSIT,
              CurrencyUtils.toMinorUnitsOrNull(
                  contract.getSecurityDeposit(), contract.getSecurityDepositCurrency()))
          .set(CONTRACTS.SECURITY_DEPOSIT_CURRENCY, contract.getSecurityDepositCurrency())
          .set(CONTRACTS.PAYMENT_FREQUENCY, contract.getPaymentFrequency().name())
          .set(CONTRACTS.PAYMENT_DUE_DAY, contract.getPaymentDueDay())
          .set(CONTRACTS.AUTO_RENEWAL, contract.getAutoRenewal())
          .set(CONTRACTS.RENEWAL_NOTICE_DAYS, contract.getRenewalNoticeDays())
          .set(CONTRACTS.TERMINATION_NOTICE_DAYS, contract.getTerminationNoticeDays())
          .set(CONTRACTS.LATE_FEE_PERCENTAGE, contract.getLateFeePercentage())
          .set(CONTRACTS.STATUS, contract.getStatus().name())
          .set(CONTRACTS.TERMS_AND_CONDITIONS, contract.getTermsAndConditions())
          .set(CONTRACTS.NOTES, contract.getNotes())
          .set(CONTRACTS.CREATED_AT, createdAt)
          .set(CONTRACTS.UPDATED_AT, updatedAt)
          .set(CONTRACTS.CREATED_BY, contract.getCreatedBy())
          .set(CONTRACTS.UPDATED_BY, contract.getUpdatedBy())
          .execute();

      contract.setId(id);
      contract.setCreatedAt(createdAt.toInstant(UTC));
      contract.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      // Update
      LocalDateTime updatedAt =
          contract.getUpdatedAt() != null
              ? LocalDateTime.ofInstant(contract.getUpdatedAt(), UTC)
              : now;

      dsl.update(CONTRACTS)
          .set(CONTRACTS.PROPERTY_ID, contract.getPropertyId())
          .set(CONTRACTS.CONTRACT_TYPE, contract.getContractType().name())
          .set(CONTRACTS.START_DATE, contract.getStartDate())
          .set(CONTRACTS.END_DATE, contract.getEndDate())
          .set(CONTRACTS.SIGNED_DATE, contract.getSignedDate())
          .set(
              CONTRACTS.RENT_AMOUNT,
              CurrencyUtils.toMinorUnits(
                  contract.getRentAmount(), contract.getRentAmountCurrency()))
          .set(CONTRACTS.RENT_AMOUNT_CURRENCY, contract.getRentAmountCurrency())
          .set(
              CONTRACTS.DEPOSIT_AMOUNT,
              CurrencyUtils.toMinorUnitsOrNull(
                  contract.getDepositAmount(), contract.getDepositAmountCurrency()))
          .set(CONTRACTS.DEPOSIT_AMOUNT_CURRENCY, contract.getDepositAmountCurrency())
          .set(
              CONTRACTS.SECURITY_DEPOSIT,
              CurrencyUtils.toMinorUnitsOrNull(
                  contract.getSecurityDeposit(), contract.getSecurityDepositCurrency()))
          .set(CONTRACTS.SECURITY_DEPOSIT_CURRENCY, contract.getSecurityDepositCurrency())
          .set(CONTRACTS.PAYMENT_FREQUENCY, contract.getPaymentFrequency().name())
          .set(CONTRACTS.PAYMENT_DUE_DAY, contract.getPaymentDueDay())
          .set(CONTRACTS.AUTO_RENEWAL, contract.getAutoRenewal())
          .set(CONTRACTS.RENEWAL_NOTICE_DAYS, contract.getRenewalNoticeDays())
          .set(CONTRACTS.TERMINATION_NOTICE_DAYS, contract.getTerminationNoticeDays())
          .set(CONTRACTS.LATE_FEE_PERCENTAGE, contract.getLateFeePercentage())
          .set(CONTRACTS.STATUS, contract.getStatus().name())
          .set(CONTRACTS.TERMS_AND_CONDITIONS, contract.getTermsAndConditions())
          .set(CONTRACTS.NOTES, contract.getNotes())
          .set(CONTRACTS.UPDATED_AT, updatedAt)
          .set(CONTRACTS.UPDATED_BY, contract.getUpdatedBy())
          .where(CONTRACTS.ID.eq(contract.getId()).and(CONTRACTS.TEAM_ID.eq(contract.getTeamId())))
          .execute();

      contract.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return contract;
  }

  public PaginatedResult<Contract> findAllByTeamIdPaginated(
      UUID teamId, String status, UUID propertyId, UUID tenantId, PageRequest pageRequest) {
    Condition condition = CONTRACTS.TEAM_ID.eq(teamId).and(CONTRACTS.DELETED_AT.isNull());
    if (status != null && !status.isEmpty()) {
      condition = condition.and(CONTRACTS.STATUS.eq(status));
    }
    if (propertyId != null) {
      condition = condition.and(CONTRACTS.PROPERTY_ID.eq(propertyId));
    }
    if (tenantId != null) {
      var CP_CONTRACT_ID = org.jooq.impl.DSL.field("contract_parties.contract_id", UUID.class);
      var CP_TENANT_ID = org.jooq.impl.DSL.field("contract_parties.tenant_id", UUID.class);
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
                              .and(CP_TENANT_ID.eq(tenantId))
                              .and(CP_TEAM_ID.eq(teamId))
                              .and(CP_DELETED_AT.isNull()))));
    }
    Map<String, Field<?>> sortableFields =
        Map.of(
            "createdAt", CONTRACTS.CREATED_AT,
            "startDate", CONTRACTS.START_DATE,
            "endDate", CONTRACTS.END_DATE,
            "rentAmount", CONTRACTS.RENT_AMOUNT,
            "status", CONTRACTS.STATUS);
    return PaginationHelper.paginate(
        dsl,
        CONTRACTS,
        condition,
        sortableFields,
        CONTRACTS.CREATED_AT,
        pageRequest,
        r -> mapper.toDomain((ContractsRecord) r));
  }

  public List<Contract> findActiveByTeamId(UUID teamId) {
    return dsl.selectFrom(CONTRACTS)
        .where(
            CONTRACTS
                .TEAM_ID
                .eq(teamId)
                .and(CONTRACTS.STATUS.eq(ACTIVE.name()))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .fetch()
        .map(mapper::toDomain);
  }

  public List<Record> findActiveContractIncomeByTeamId(UUID teamId) {
    return dsl
        .select(CONTRACTS.RENT_AMOUNT, CONTRACTS.RENT_AMOUNT_CURRENCY, CONTRACTS.PAYMENT_FREQUENCY)
        .from(CONTRACTS)
        .where(
            CONTRACTS
                .TEAM_ID
                .eq(teamId)
                .and(CONTRACTS.STATUS.eq("ACTIVE"))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .fetch()
        .stream()
        .map(r -> (Record) r)
        .toList();
  }

  public List<Contract> findExpiringContracts(UUID teamId, LocalDate beforeDate) {
    return dsl.selectFrom(CONTRACTS)
        .where(
            CONTRACTS
                .TEAM_ID
                .eq(teamId)
                .and(CONTRACTS.STATUS.eq(ACTIVE.name()))
                .and(CONTRACTS.END_DATE.isNotNull())
                .and(CONTRACTS.END_DATE.le(beforeDate))
                .and(CONTRACTS.DELETED_AT.isNull()))
        .fetch()
        .map(mapper::toDomain);
  }

  public LocalDate findEarliestStartDate(UUID teamId) {
    return dsl.select(min(CONTRACTS.START_DATE))
        .from(CONTRACTS)
        .where(
            CONTRACTS
                .TEAM_ID
                .eq(teamId)
                .and(CONTRACTS.DELETED_AT.isNull())
                .and(CONTRACTS.START_DATE.isNotNull()))
        .fetchOne(min(CONTRACTS.START_DATE));
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(CONTRACTS)
        .set(CONTRACTS.DELETED_AT, now)
        .where(CONTRACTS.ID.eq(id).and(CONTRACTS.TEAM_ID.eq(teamId)))
        .execute();
  }
}
