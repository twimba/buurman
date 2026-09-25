package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.DEPOSITS;
import static com.buurman.jooq.generated.Tables.DEPOSIT_DEDUCTIONS;
import static java.time.ZoneOffset.UTC;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Deposit;
import com.buurman.domain.DepositDeduction;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.jooq.generated.tables.records.DepositDeductionsRecord;
import com.buurman.jooq.generated.tables.records.DepositsRecord;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DepositRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public Optional<Deposit> findByContractIdAndTeamId(UUID contractId, UUID teamId) {
    return dsl.selectFrom(DEPOSITS)
        .where(
            DEPOSITS
                .CONTRACT_ID
                .eq(contractId)
                .and(DEPOSITS.TEAM_ID.eq(teamId))
                .and(DEPOSITS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public List<Deposit> findAllByTeamId(UUID teamId) {
    return dsl.selectFrom(DEPOSITS)
        .where(DEPOSITS.TEAM_ID.eq(teamId).and(DEPOSITS.DELETED_AT.isNull()))
        .orderBy(DEPOSITS.CREATED_AT.desc())
        .fetch()
        .map(this::toDomain);
  }

  public List<DepositDeduction> findAllDeductionsByTeamId(UUID teamId) {
    return dsl.selectFrom(DEPOSIT_DEDUCTIONS)
        .where(DEPOSIT_DEDUCTIONS.TEAM_ID.eq(teamId).and(DEPOSIT_DEDUCTIONS.DELETED_AT.isNull()))
        .orderBy(DEPOSIT_DEDUCTIONS.DEDUCTION_DATE.asc())
        .fetch()
        .map(this::toDomain);
  }

  public Deposit getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(DEPOSITS)
        .where(
            DEPOSITS
                .IDENTIFIER
                .eq(identifier)
                .and(DEPOSITS.TEAM_ID.eq(teamId))
                .and(DEPOSITS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain)
        .orElseThrow(() -> new NotFoundException("Deposit not found"));
  }

  public Deposit save(Deposit deposit) {
    LocalDateTime now = LocalDateTime.now(clock);
    long returnedMinor =
        MoneyAmount.of(deposit.getReturnedAmount(), deposit.getAmount().currency()).toMinorUnits();
    if (deposit.getId() == null) {
      UUID id = UUID.randomUUID();
      dsl.insertInto(DEPOSITS)
          .set(DEPOSITS.ID, id)
          .set(DEPOSITS.IDENTIFIER, deposit.getIdentifier().orElseThrow())
          .set(DEPOSITS.TEAM_ID, deposit.getTeamId())
          .set(DEPOSITS.CONTRACT_ID, deposit.getContractId())
          .set(DEPOSITS.CONTACT_ID, deposit.getContactId().orElse(null))
          .set(DEPOSITS.AMOUNT, deposit.getAmount().value())
          .set(DEPOSITS.CURRENCY, deposit.getAmount().currency())
          .set(DEPOSITS.RECEIVED_DATE, deposit.getReceivedDate().orElse(null))
          .set(DEPOSITS.HELD_WHERE, deposit.getHeldWhere().orElse(null))
          .set(DEPOSITS.STATUS, deposit.getStatus().name())
          .set(DEPOSITS.RETURN_DUE_DATE, deposit.getReturnDueDate().orElse(null))
          .set(DEPOSITS.RETURNED_DATE, deposit.getReturnedDate().orElse(null))
          .set(DEPOSITS.RETURNED_AMOUNT, returnedMinor)
          .set(DEPOSITS.NOTES, deposit.getNotes().orElse(null))
          .set(DEPOSITS.CREATED_AT, now)
          .set(DEPOSITS.UPDATED_AT, now)
          .set(DEPOSITS.CREATED_BY, deposit.getCreatedBy())
          .set(DEPOSITS.UPDATED_BY, deposit.getUpdatedBy())
          .execute();
      deposit.setId(id);
      deposit.setCreatedAt(now.toInstant(UTC));
      deposit.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(DEPOSITS)
          .set(DEPOSITS.CONTACT_ID, deposit.getContactId().orElse(null))
          .set(DEPOSITS.AMOUNT, deposit.getAmount().value())
          .set(DEPOSITS.CURRENCY, deposit.getAmount().currency())
          .set(DEPOSITS.RECEIVED_DATE, deposit.getReceivedDate().orElse(null))
          .set(DEPOSITS.HELD_WHERE, deposit.getHeldWhere().orElse(null))
          .set(DEPOSITS.STATUS, deposit.getStatus().name())
          .set(DEPOSITS.RETURN_DUE_DATE, deposit.getReturnDueDate().orElse(null))
          .set(DEPOSITS.RETURNED_DATE, deposit.getReturnedDate().orElse(null))
          .set(DEPOSITS.RETURNED_AMOUNT, returnedMinor)
          .set(DEPOSITS.NOTES, deposit.getNotes().orElse(null))
          .set(DEPOSITS.UPDATED_AT, now)
          .set(DEPOSITS.UPDATED_BY, deposit.getUpdatedBy())
          .where(DEPOSITS.ID.eq(deposit.getId()).and(DEPOSITS.TEAM_ID.eq(deposit.getTeamId())))
          .execute();
      deposit.setUpdatedAt(now.toInstant(UTC));
    }
    return deposit;
  }

  public List<DepositDeduction> findDeductions(UUID depositId, UUID teamId) {
    return dsl.selectFrom(DEPOSIT_DEDUCTIONS)
        .where(
            DEPOSIT_DEDUCTIONS
                .DEPOSIT_ID
                .eq(depositId)
                .and(DEPOSIT_DEDUCTIONS.TEAM_ID.eq(teamId))
                .and(DEPOSIT_DEDUCTIONS.DELETED_AT.isNull()))
        .orderBy(DEPOSIT_DEDUCTIONS.DEDUCTION_DATE.asc(), DEPOSIT_DEDUCTIONS.CREATED_AT.asc())
        .fetch()
        .map(this::toDomain);
  }

  public DepositDeduction saveDeduction(DepositDeduction deduction) {
    LocalDateTime now = LocalDateTime.now(clock);
    UUID id = UUID.randomUUID();
    dsl.insertInto(DEPOSIT_DEDUCTIONS)
        .set(DEPOSIT_DEDUCTIONS.ID, id)
        .set(DEPOSIT_DEDUCTIONS.IDENTIFIER, deduction.getIdentifier().orElseThrow())
        .set(DEPOSIT_DEDUCTIONS.TEAM_ID, deduction.getTeamId())
        .set(DEPOSIT_DEDUCTIONS.DEPOSIT_ID, deduction.getDepositId())
        .set(DEPOSIT_DEDUCTIONS.AMOUNT, deduction.getAmount().value())
        .set(DEPOSIT_DEDUCTIONS.CURRENCY, deduction.getAmount().currency())
        .set(DEPOSIT_DEDUCTIONS.REASON, deduction.getReason())
        .set(DEPOSIT_DEDUCTIONS.DEDUCTION_DATE, deduction.getDeductionDate())
        .set(DEPOSIT_DEDUCTIONS.CREATED_AT, now)
        .set(DEPOSIT_DEDUCTIONS.UPDATED_AT, now)
        .set(DEPOSIT_DEDUCTIONS.CREATED_BY, deduction.getCreatedBy())
        .set(DEPOSIT_DEDUCTIONS.UPDATED_BY, deduction.getUpdatedBy())
        .execute();
    deduction.setId(id);
    deduction.setCreatedAt(now.toInstant(UTC));
    deduction.setUpdatedAt(now.toInstant(UTC));
    return deduction;
  }

  public boolean softDeleteDeduction(Sid identifier, UUID depositId, UUID teamId) {
    return dsl.update(DEPOSIT_DEDUCTIONS)
            .set(DEPOSIT_DEDUCTIONS.DELETED_AT, LocalDateTime.now(clock))
            .where(
                DEPOSIT_DEDUCTIONS
                    .IDENTIFIER
                    .eq(identifier)
                    .and(DEPOSIT_DEDUCTIONS.DEPOSIT_ID.eq(depositId))
                    .and(DEPOSIT_DEDUCTIONS.TEAM_ID.eq(teamId))
                    .and(DEPOSIT_DEDUCTIONS.DELETED_AT.isNull()))
            .execute()
        > 0;
  }

  private Deposit toDomain(DepositsRecord r) {
    String currency = r.getCurrency();
    int digits = CurrencyUtils.getFractionalDigits(currency);
    Deposit d = new Deposit();
    d.setId(r.getId());
    d.setIdentifier(Optional.of(r.getIdentifier()));
    d.setTeamId(r.getTeamId());
    d.setContractId(r.getContractId());
    d.setContactId(Optional.ofNullable(r.getContactId()));
    d.setAmount(MoneyAmount.of(r.getAmount(), currency));
    d.setReceivedDate(Optional.ofNullable(r.getReceivedDate()));
    d.setHeldWhere(Optional.ofNullable(r.getHeldWhere()));
    d.setStatus(Deposit.DepositStatus.valueOf(r.getStatus()));
    d.setReturnDueDate(Optional.ofNullable(r.getReturnDueDate()));
    d.setReturnedDate(Optional.ofNullable(r.getReturnedDate()));
    d.setReturnedAmount(
        BigDecimal.valueOf(Optional.ofNullable(r.getReturnedAmount()).orElse(0L), digits));
    d.setNotes(Optional.ofNullable(r.getNotes()));
    d.setCreatedAt(r.getCreatedAt().toInstant(UTC));
    d.setUpdatedAt(r.getUpdatedAt().toInstant(UTC));
    d.setCreatedBy(r.getCreatedBy());
    d.setUpdatedBy(r.getUpdatedBy());
    d.setDeletedAt(Optional.ofNullable(r.getDeletedAt()).map(dt -> dt.toInstant(UTC)));
    return d;
  }

  private DepositDeduction toDomain(DepositDeductionsRecord r) {
    DepositDeduction d = new DepositDeduction();
    d.setId(r.getId());
    d.setIdentifier(Optional.of(r.getIdentifier()));
    d.setTeamId(r.getTeamId());
    d.setDepositId(r.getDepositId());
    d.setAmount(MoneyAmount.of(r.getAmount(), r.getCurrency()));
    d.setReason(r.getReason());
    d.setDeductionDate(r.getDeductionDate());
    d.setCreatedAt(r.getCreatedAt().toInstant(UTC));
    d.setUpdatedAt(r.getUpdatedAt().toInstant(UTC));
    d.setCreatedBy(r.getCreatedBy());
    d.setUpdatedBy(r.getUpdatedBy());
    d.setDeletedAt(Optional.ofNullable(r.getDeletedAt()).map(dt -> dt.toInstant(UTC)));
    return d;
  }
}
