package com.buurman.repository;

import static com.buurman.util.SidGenerator.newPaymentInstructionId;
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
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PaymentInstruction;
import com.buurman.domain.Sid;
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
      Sid identifier = newPaymentInstructionId();

      dsl.insertInto(TABLE)
          .set(ID, id)
          .set(IDENTIFIER, identifier.value())
          .set(TEAM_ID, pi.getTeamId())
          .set(NAME, pi.getName())
          .set(DESCRIPTION, pi.getDescription())
          .set(PAYMENT_METHOD, pi.getPaymentMethod().name())
          .set(BANK_NAME, pi.getBankName().orElse(null))
          .set(ACCOUNT_HOLDER_NAME, pi.getAccountHolderName().orElse(null))
          .set(IBAN, pi.getIban().orElse(null))
          .set(BIC_SWIFT, pi.getBicSwift().orElse(null))
          .set(ACCOUNT_NUMBER, pi.getAccountNumber().orElse(null))
          .set(ROUTING_NUMBER, pi.getRoutingNumber().orElse(null))
          .set(PAYMENT_REFERENCE, pi.getPaymentReference().orElse(null))
          .set(ADDITIONAL_DETAILS, pi.getAdditionalDetails().orElse(null))
          .set(IS_DEFAULT, pi.getIsDefault() != null ? pi.getIsDefault() : false)
          .set(CREATED_AT, now)
          .set(UPDATED_AT, now)
          .set(CREATED_BY, pi.getCreatedBy())
          .set(UPDATED_BY, pi.getUpdatedBy())
          .execute();

      pi.setId(id);
      pi.setIdentifier(java.util.Optional.of(identifier));
      pi.setCreatedAt(now.toInstant(UTC));
      pi.setUpdatedAt(now.toInstant(UTC));
    } else {

      dsl.update(TABLE)
          .set(NAME, pi.getName())
          .set(DESCRIPTION, pi.getDescription())
          .set(PAYMENT_METHOD, pi.getPaymentMethod().name())
          .set(BANK_NAME, pi.getBankName().orElse(null))
          .set(ACCOUNT_HOLDER_NAME, pi.getAccountHolderName().orElse(null))
          .set(IBAN, pi.getIban().orElse(null))
          .set(BIC_SWIFT, pi.getBicSwift().orElse(null))
          .set(ACCOUNT_NUMBER, pi.getAccountNumber().orElse(null))
          .set(ROUTING_NUMBER, pi.getRoutingNumber().orElse(null))
          .set(PAYMENT_REFERENCE, pi.getPaymentReference().orElse(null))
          .set(ADDITIONAL_DETAILS, pi.getAdditionalDetails().orElse(null))
          .set(IS_DEFAULT, pi.getIsDefault() != null ? pi.getIsDefault() : false)
          .set(UPDATED_AT, now)
          .set(UPDATED_BY, pi.getUpdatedBy())
          .where(ID.eq(pi.getId()).and(TEAM_ID.eq(pi.getTeamId())).and(DELETED_AT.isNull()))
          .execute();
    }
    return pi;
  }

  public List<PaymentInstruction> findAllByTeamId(UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(TABLE)
            .where(TEAM_ID.eq(teamId).and(DELETED_AT.isNull()))
            .orderBy(CREATED_AT.asc())
            .fetch()
            .map(this::toDomain));
  }

  public Optional<PaymentInstruction> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(TABLE)
        .where(IDENTIFIER.eq(identifier.value()).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public PaymentInstruction getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
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
    pi.setIdentifier(java.util.Optional.of(com.buurman.domain.Sid.of(record.get(IDENTIFIER))));
    pi.setTeamId(record.get(TEAM_ID));
    pi.setName(record.get(NAME));
    pi.setDescription(record.get(DESCRIPTION));
    pi.setPaymentMethod(PaymentInstruction.PaymentMethod.valueOf(record.get(PAYMENT_METHOD)));
    pi.setBankName(Optional.of(record.get(BANK_NAME)));
    pi.setAccountHolderName(Optional.of(record.get(ACCOUNT_HOLDER_NAME)));
    pi.setIban(Optional.of(record.get(IBAN)));
    pi.setBicSwift(Optional.of(record.get(BIC_SWIFT)));
    pi.setAccountNumber(Optional.of(record.get(ACCOUNT_NUMBER)));
    pi.setRoutingNumber(Optional.of(record.get(ROUTING_NUMBER)));
    pi.setPaymentReference(Optional.of(record.get(PAYMENT_REFERENCE)));
    pi.setAdditionalDetails(Optional.of(record.get(ADDITIONAL_DETAILS)));
    pi.setIsDefault(record.get(IS_DEFAULT));
    Instant createdAt = toInstant(record.get("created_at"));
    if (createdAt != null) {
      pi.setCreatedAt(createdAt);
    }
    Instant updatedAt = toInstant(record.get("updated_at"));
    if (updatedAt != null) {
      pi.setUpdatedAt(updatedAt);
    }
    pi.setCreatedBy(record.get(CREATED_BY));
    pi.setUpdatedBy(record.get(UPDATED_BY));
    pi.setDeletedAt(Optional.ofNullable(toInstant(record.get("deleted_at"))));
    return pi;
  }

  private static @Nullable Instant toInstant(@Nullable Object val) {
    if (val instanceof LocalDateTime ldt) {
      return ldt.toInstant(UTC);
    }
    if (val instanceof java.sql.Timestamp ts) {
      return ts.toInstant();
    }
    return null;
  }
}
