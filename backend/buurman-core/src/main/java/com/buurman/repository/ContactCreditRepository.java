package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.CONTACT_CREDITS;
import static java.time.ZoneOffset.UTC;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.ContactCredit;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.jooq.generated.tables.records.ContactCreditsRecord;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ContactCreditRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public List<ContactCredit> findByContactIdAndTeamId(UUID contactId, UUID teamId) {
    return dsl.selectFrom(CONTACT_CREDITS)
        .where(
            CONTACT_CREDITS
                .CONTACT_ID
                .eq(contactId)
                .and(CONTACT_CREDITS.TEAM_ID.eq(teamId))
                .and(CONTACT_CREDITS.DELETED_AT.isNull()))
        .orderBy(CONTACT_CREDITS.CREATED_AT.desc())
        .fetch()
        .map(this::toDomain);
  }

  /** Credits with money left to apply, oldest first, for a contact. */
  public List<ContactCredit> findOpenByContactIdAndTeamId(UUID contactId, UUID teamId) {
    return dsl.selectFrom(CONTACT_CREDITS)
        .where(
            CONTACT_CREDITS
                .CONTACT_ID
                .eq(contactId)
                .and(CONTACT_CREDITS.TEAM_ID.eq(teamId))
                .and(CONTACT_CREDITS.REMAINING_AMOUNT.gt(0L))
                .and(CONTACT_CREDITS.DELETED_AT.isNull()))
        .orderBy(CONTACT_CREDITS.CREATED_AT.asc())
        .fetch()
        .map(this::toDomain);
  }

  public Optional<ContactCredit> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(CONTACT_CREDITS)
        .where(
            CONTACT_CREDITS
                .IDENTIFIER
                .eq(identifier)
                .and(CONTACT_CREDITS.TEAM_ID.eq(teamId))
                .and(CONTACT_CREDITS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public ContactCredit getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Credit not found"));
  }

  public ContactCredit save(ContactCredit credit) {
    LocalDateTime now = LocalDateTime.now(clock);
    long remainingMinor =
        MoneyAmount.of(credit.getRemainingAmount(), credit.getAmount().currency()).toMinorUnits();
    if (credit.getId() == null) {
      UUID id = UUID.randomUUID();
      dsl.insertInto(CONTACT_CREDITS)
          .set(CONTACT_CREDITS.ID, id)
          .set(CONTACT_CREDITS.IDENTIFIER, credit.getIdentifier().orElseThrow())
          .set(CONTACT_CREDITS.TEAM_ID, credit.getTeamId())
          .set(CONTACT_CREDITS.CONTACT_ID, credit.getContactId())
          .set(CONTACT_CREDITS.CONTRACT_ID, credit.getContractId().orElse(null))
          .set(CONTACT_CREDITS.AMOUNT, credit.getAmount().value())
          .set(CONTACT_CREDITS.REMAINING_AMOUNT, remainingMinor)
          .set(CONTACT_CREDITS.CURRENCY, credit.getAmount().currency())
          .set(CONTACT_CREDITS.SOURCE, credit.getSource().name())
          .set(CONTACT_CREDITS.REASON, credit.getReason().orElse(null))
          .set(CONTACT_CREDITS.SOURCE_PAYMENT_ID, credit.getSourcePaymentId().orElse(null))
          .set(CONTACT_CREDITS.CREATED_AT, now)
          .set(CONTACT_CREDITS.UPDATED_AT, now)
          .set(CONTACT_CREDITS.CREATED_BY, credit.getCreatedBy())
          .set(CONTACT_CREDITS.UPDATED_BY, credit.getUpdatedBy())
          .execute();
      credit.setId(id);
      credit.setCreatedAt(now.toInstant(UTC));
      credit.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(CONTACT_CREDITS)
          .set(CONTACT_CREDITS.REMAINING_AMOUNT, remainingMinor)
          .set(CONTACT_CREDITS.REASON, credit.getReason().orElse(null))
          .set(
              CONTACT_CREDITS.REFUNDED_AT,
              credit.getRefundedAt().map(i -> LocalDateTime.ofInstant(i, UTC)).orElse(null))
          .set(CONTACT_CREDITS.REFUND_NOTES, credit.getRefundNotes().orElse(null))
          .set(CONTACT_CREDITS.UPDATED_AT, now)
          .set(CONTACT_CREDITS.UPDATED_BY, credit.getUpdatedBy())
          .where(
              CONTACT_CREDITS
                  .ID
                  .eq(credit.getId())
                  .and(CONTACT_CREDITS.TEAM_ID.eq(credit.getTeamId())))
          .execute();
      credit.setUpdatedAt(now.toInstant(UTC));
    }
    return credit;
  }

  private ContactCredit toDomain(ContactCreditsRecord record) {
    String currency = record.getCurrency();
    int digits = CurrencyUtils.getFractionalDigits(currency);
    BigDecimal remaining =
        BigDecimal.valueOf(Optional.ofNullable(record.getRemainingAmount()).orElse(0L), digits);
    ContactCredit credit = new ContactCredit();
    credit.setId(record.getId());
    credit.setIdentifier(Optional.of(record.getIdentifier()));
    credit.setTeamId(record.getTeamId());
    credit.setContactId(record.getContactId());
    credit.setContractId(Optional.ofNullable(record.getContractId()));
    credit.setAmount(MoneyAmount.of(record.getAmount(), currency));
    credit.setRemainingAmount(remaining);
    credit.setSource(ContactCredit.CreditSource.valueOf(record.getSource()));
    credit.setReason(Optional.ofNullable(record.getReason()));
    credit.setSourcePaymentId(Optional.ofNullable(record.getSourcePaymentId()));
    credit.setRefundedAt(Optional.ofNullable(record.getRefundedAt()).map(dt -> dt.toInstant(UTC)));
    credit.setRefundNotes(Optional.ofNullable(record.getRefundNotes()));
    credit.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    credit.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    credit.setCreatedBy(record.getCreatedBy());
    credit.setUpdatedBy(record.getUpdatedBy());
    credit.setDeletedAt(Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));
    return credit;
  }
}
