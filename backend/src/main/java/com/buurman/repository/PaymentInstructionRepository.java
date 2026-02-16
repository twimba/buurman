package com.buurman.repository;

import static com.buurman.util.UlidGenerator.newPaymentInstructionId;
import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PaymentInstruction;
import com.buurman.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentInstructionRepository {

  private static final Table<?> TABLE = table("payment_instructions");
  private static final Field<UUID> ID = field("id", UUID.class);
  private static final Field<String> IDENTIFIER = field("identifier", String.class);
  private static final Field<UUID> TEAM_ID = field("team_id", UUID.class);
  private static final Field<String> NAME = field("name", String.class);
  private static final Field<String> DESCRIPTION = field("description", String.class);
  private static final Field<String> PAYMENT_METHOD = field("payment_method", String.class);
  private static final Field<String> BANK_NAME = field("bank_name", String.class);
  private static final Field<String> ACCOUNT_HOLDER_NAME =
      field("account_holder_name", String.class);
  private static final Field<String> IBAN = field("iban", String.class);
  private static final Field<String> BIC_SWIFT = field("bic_swift", String.class);
  private static final Field<String> ACCOUNT_NUMBER = field("account_number", String.class);
  private static final Field<String> ROUTING_NUMBER = field("routing_number", String.class);
  private static final Field<String> PAYMENT_REFERENCE = field("payment_reference", String.class);
  private static final Field<String> ADDITIONAL_DETAILS = field("additional_details", String.class);
  private static final Field<Boolean> IS_DEFAULT = field("is_default", Boolean.class);
  private static final Field<LocalDateTime> CREATED_AT = field("created_at", LocalDateTime.class);
  private static final Field<LocalDateTime> UPDATED_AT = field("updated_at", LocalDateTime.class);
  private static final Field<UUID> CREATED_BY = field("created_by", UUID.class);
  private static final Field<UUID> UPDATED_BY = field("updated_by", UUID.class);
  private static final Field<LocalDateTime> DELETED_AT = field("deleted_at", LocalDateTime.class);

  private final DSLContext dsl;
  private final Clock clock;

  public PaymentInstruction save(PaymentInstruction pi) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (pi.getId() == null) {
      UUID id = UUID.randomUUID();
      String identifier = newPaymentInstructionId().value();
      LocalDateTime createdAt =
          pi.getCreatedAt() != null ? LocalDateTime.ofInstant(pi.getCreatedAt(), UTC) : now;
      LocalDateTime updatedAt =
          pi.getUpdatedAt() != null ? LocalDateTime.ofInstant(pi.getUpdatedAt(), UTC) : now;

      dsl.insertInto(TABLE)
          .set(ID, id)
          .set(IDENTIFIER, identifier)
          .set(TEAM_ID, pi.getTeamId())
          .set(NAME, pi.getName())
          .set(DESCRIPTION, pi.getDescription())
          .set(PAYMENT_METHOD, pi.getPaymentMethod().name())
          .set(BANK_NAME, pi.getBankName())
          .set(ACCOUNT_HOLDER_NAME, pi.getAccountHolderName())
          .set(IBAN, pi.getIban())
          .set(BIC_SWIFT, pi.getBicSwift())
          .set(ACCOUNT_NUMBER, pi.getAccountNumber())
          .set(ROUTING_NUMBER, pi.getRoutingNumber())
          .set(PAYMENT_REFERENCE, pi.getPaymentReference())
          .set(ADDITIONAL_DETAILS, pi.getAdditionalDetails())
          .set(IS_DEFAULT, pi.getIsDefault() != null ? pi.getIsDefault() : false)
          .set(CREATED_AT, createdAt)
          .set(UPDATED_AT, updatedAt)
          .set(CREATED_BY, pi.getCreatedBy())
          .set(UPDATED_BY, pi.getUpdatedBy())
          .execute();

      pi.setId(id);
      pi.setIdentifier(identifier);
      pi.setCreatedAt(createdAt.toInstant(UTC));
      pi.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      LocalDateTime updatedAt =
          pi.getUpdatedAt() != null ? LocalDateTime.ofInstant(pi.getUpdatedAt(), UTC) : now;

      dsl.update(TABLE)
          .set(NAME, pi.getName())
          .set(DESCRIPTION, pi.getDescription())
          .set(PAYMENT_METHOD, pi.getPaymentMethod().name())
          .set(BANK_NAME, pi.getBankName())
          .set(ACCOUNT_HOLDER_NAME, pi.getAccountHolderName())
          .set(IBAN, pi.getIban())
          .set(BIC_SWIFT, pi.getBicSwift())
          .set(ACCOUNT_NUMBER, pi.getAccountNumber())
          .set(ROUTING_NUMBER, pi.getRoutingNumber())
          .set(PAYMENT_REFERENCE, pi.getPaymentReference())
          .set(ADDITIONAL_DETAILS, pi.getAdditionalDetails())
          .set(IS_DEFAULT, pi.getIsDefault() != null ? pi.getIsDefault() : false)
          .set(UPDATED_AT, updatedAt)
          .set(UPDATED_BY, pi.getUpdatedBy())
          .where(ID.eq(pi.getId()).and(TEAM_ID.eq(pi.getTeamId())).and(DELETED_AT.isNull()))
          .execute();
    }
    return pi;
  }

  public List<PaymentInstruction> findAllByTeamId(UUID teamId) {
    return dsl.selectFrom(TABLE)
        .where(TEAM_ID.eq(teamId).and(DELETED_AT.isNull()))
        .orderBy(CREATED_AT.asc())
        .fetch()
        .map(this::toDomain);
  }

  public Optional<PaymentInstruction> findByIdentifierAndTeamId(String identifier, UUID teamId) {
    return dsl.selectFrom(TABLE)
        .where(IDENTIFIER.eq(identifier).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public PaymentInstruction getByIdentifierAndTeamId(String identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Payment instruction not found"));
  }

  public Optional<PaymentInstruction> findByIdAndTeamId(UUID id, UUID teamId) {
    return dsl.selectFrom(TABLE)
        .where(ID.eq(id).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public PaymentInstruction getByIdAndTeamId(UUID id, UUID teamId) {
    return findByIdAndTeamId(id, teamId)
        .orElseThrow(() -> new NotFoundException("Payment instruction not found"));
  }

  public Optional<PaymentInstruction> findDefaultByTeamId(UUID teamId) {
    return dsl.selectFrom(TABLE)
        .where(TEAM_ID.eq(teamId).and(IS_DEFAULT.eq(true)).and(DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public void clearDefaultByTeamId(UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(TABLE)
        .set(IS_DEFAULT, false)
        .set(UPDATED_AT, now)
        .where(TEAM_ID.eq(teamId).and(IS_DEFAULT.eq(true)).and(DELETED_AT.isNull()))
        .execute();
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(TABLE).set(DELETED_AT, now).where(ID.eq(id).and(TEAM_ID.eq(teamId))).execute();
  }

  private PaymentInstruction toDomain(Record record) {
    PaymentInstruction pi = new PaymentInstruction();
    pi.setId(record.get(ID));
    pi.setIdentifier(record.get(IDENTIFIER));
    pi.setTeamId(record.get(TEAM_ID));
    pi.setName(record.get(NAME));
    pi.setDescription(record.get(DESCRIPTION));
    pi.setPaymentMethod(PaymentInstruction.PaymentMethod.valueOf(record.get(PAYMENT_METHOD)));
    pi.setBankName(record.get(BANK_NAME));
    pi.setAccountHolderName(record.get(ACCOUNT_HOLDER_NAME));
    pi.setIban(record.get(IBAN));
    pi.setBicSwift(record.get(BIC_SWIFT));
    pi.setAccountNumber(record.get(ACCOUNT_NUMBER));
    pi.setRoutingNumber(record.get(ROUTING_NUMBER));
    pi.setPaymentReference(record.get(PAYMENT_REFERENCE));
    pi.setAdditionalDetails(record.get(ADDITIONAL_DETAILS));
    pi.setIsDefault(record.get(IS_DEFAULT));
    pi.setCreatedAt(toInstant(record.get("created_at")));
    pi.setUpdatedAt(toInstant(record.get("updated_at")));
    pi.setCreatedBy(record.get(CREATED_BY));
    pi.setUpdatedBy(record.get(UPDATED_BY));
    pi.setDeletedAt(toInstant(record.get("deleted_at")));
    return pi;
  }

  private static Instant toInstant(Object val) {
    if (val instanceof LocalDateTime ldt) {
      return ldt.toInstant(UTC);
    }
    if (val instanceof java.sql.Timestamp ts) {
      return ts.toInstant();
    }
    return null;
  }
}
