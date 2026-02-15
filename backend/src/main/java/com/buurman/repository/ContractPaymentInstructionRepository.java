package com.buurman.repository;

import com.buurman.domain.ContractPaymentInstruction;
import com.buurman.util.EntityPrefix;
import com.buurman.util.UlidGenerator;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.springframework.stereotype.Repository;

import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ContractPaymentInstructionRepository {

    private static final Table<?> TABLE = table("contract_payment_instructions");
    private static final Field<UUID> ID = field("id", UUID.class);
    private static final Field<String> IDENTIFIER = field("identifier", String.class);
    private static final Field<UUID> TEAM_ID = field("team_id", UUID.class);
    private static final Field<UUID> CONTRACT_ID = field("contract_id", UUID.class);
    private static final Field<UUID> PAYMENT_INSTRUCTION_ID = field("payment_instruction_id", UUID.class);
    private static final Field<Boolean> IS_CUSTOM = field("is_custom", Boolean.class);
    private static final Field<String> CUSTOM_NAME = field("custom_name", String.class);
    private static final Field<String> CUSTOM_DESCRIPTION = field("custom_description", String.class);
    private static final Field<String> CUSTOM_PAYMENT_METHOD = field("custom_payment_method", String.class);
    private static final Field<String> CUSTOM_BANK_NAME = field("custom_bank_name", String.class);
    private static final Field<String> CUSTOM_ACCOUNT_HOLDER_NAME = field("custom_account_holder_name", String.class);
    private static final Field<String> CUSTOM_IBAN = field("custom_iban", String.class);
    private static final Field<String> CUSTOM_BIC_SWIFT = field("custom_bic_swift", String.class);
    private static final Field<String> CUSTOM_ACCOUNT_NUMBER = field("custom_account_number", String.class);
    private static final Field<String> CUSTOM_ROUTING_NUMBER = field("custom_routing_number", String.class);
    private static final Field<String> CUSTOM_PAYMENT_REFERENCE = field("custom_payment_reference", String.class);
    private static final Field<String> CUSTOM_ADDITIONAL_DETAILS = field("custom_additional_details", String.class);
    private static final Field<LocalDate> EFFECTIVE_FROM = field("effective_from", LocalDate.class);
    private static final Field<LocalDate> EFFECTIVE_TO = field("effective_to", LocalDate.class);
    private static final Field<String> NOTES = field("notes", String.class);
    private static final Field<LocalDateTime> CREATED_AT = field("created_at", LocalDateTime.class);
    private static final Field<LocalDateTime> UPDATED_AT = field("updated_at", LocalDateTime.class);
    private static final Field<UUID> CREATED_BY = field("created_by", UUID.class);
    private static final Field<UUID> UPDATED_BY = field("updated_by", UUID.class);
    private static final Field<LocalDateTime> DELETED_AT = field("deleted_at", LocalDateTime.class);

    private final DSLContext dsl;
    private final Clock clock;

    public ContractPaymentInstructionRepository(DSLContext dsl, Clock clock) {
        this.dsl = dsl;
        this.clock = clock;
    }

    public ContractPaymentInstruction save(ContractPaymentInstruction cpi) {
        LocalDateTime now = LocalDateTime.now(clock);

        UUID id = UUID.randomUUID();
        String identifier = UlidGenerator.generate(EntityPrefix.CPI);
        LocalDateTime createdAt = cpi.getCreatedAt() != null
                ? LocalDateTime.ofInstant(cpi.getCreatedAt(), UTC)
                : now;
        LocalDateTime updatedAt = cpi.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(cpi.getUpdatedAt(), UTC)
                : now;

        dsl.insertInto(TABLE)
                .set(ID, id)
                .set(IDENTIFIER, identifier)
                .set(TEAM_ID, cpi.getTeamId())
                .set(CONTRACT_ID, cpi.getContractId())
                .set(PAYMENT_INSTRUCTION_ID, cpi.getPaymentInstructionId())
                .set(IS_CUSTOM, cpi.getIsCustom())
                .set(CUSTOM_NAME, cpi.getCustomName())
                .set(CUSTOM_DESCRIPTION, cpi.getCustomDescription())
                .set(CUSTOM_PAYMENT_METHOD, cpi.getCustomPaymentMethod())
                .set(CUSTOM_BANK_NAME, cpi.getCustomBankName())
                .set(CUSTOM_ACCOUNT_HOLDER_NAME, cpi.getCustomAccountHolderName())
                .set(CUSTOM_IBAN, cpi.getCustomIban())
                .set(CUSTOM_BIC_SWIFT, cpi.getCustomBicSwift())
                .set(CUSTOM_ACCOUNT_NUMBER, cpi.getCustomAccountNumber())
                .set(CUSTOM_ROUTING_NUMBER, cpi.getCustomRoutingNumber())
                .set(CUSTOM_PAYMENT_REFERENCE, cpi.getCustomPaymentReference())
                .set(CUSTOM_ADDITIONAL_DETAILS, cpi.getCustomAdditionalDetails())
                .set(EFFECTIVE_FROM, cpi.getEffectiveFrom())
                .set(EFFECTIVE_TO, cpi.getEffectiveTo())
                .set(NOTES, cpi.getNotes())
                .set(CREATED_AT, createdAt)
                .set(UPDATED_AT, updatedAt)
                .set(CREATED_BY, cpi.getCreatedBy())
                .set(UPDATED_BY, cpi.getUpdatedBy())
                .execute();

        cpi.setId(id);
        cpi.setIdentifier(identifier);
        cpi.setCreatedAt(createdAt.toInstant(UTC));
        cpi.setUpdatedAt(updatedAt.toInstant(UTC));
        return cpi;
    }

    public List<ContractPaymentInstruction> findByContractIdAndTeamId(UUID contractId, UUID teamId) {
        return dsl.selectFrom(TABLE)
                .where(CONTRACT_ID.eq(contractId)
                        .and(TEAM_ID.eq(teamId))
                        .and(DELETED_AT.isNull()))
                .orderBy(EFFECTIVE_FROM.desc())
                .fetch()
                .map(this::toDomain);
    }

    public Optional<ContractPaymentInstruction> findCurrentByContractIdAndTeamId(UUID contractId, UUID teamId) {
        return dsl.selectFrom(TABLE)
                .where(CONTRACT_ID.eq(contractId)
                        .and(TEAM_ID.eq(teamId))
                        .and(EFFECTIVE_TO.isNull())
                        .and(DELETED_AT.isNull()))
                .fetchOptional()
                .map(this::toDomain);
    }

    public Optional<ContractPaymentInstruction> findByIdentifierAndTeamId(String identifier, UUID teamId) {
        return dsl.selectFrom(TABLE)
                .where(IDENTIFIER.eq(identifier)
                        .and(TEAM_ID.eq(teamId))
                        .and(DELETED_AT.isNull()))
                .fetchOptional()
                .map(this::toDomain);
    }

    public Optional<ContractPaymentInstruction> findByIdAndTeamId(UUID id, UUID teamId) {
        return dsl.selectFrom(TABLE)
                .where(ID.eq(id)
                        .and(TEAM_ID.eq(teamId))
                        .and(DELETED_AT.isNull()))
                .fetchOptional()
                .map(this::toDomain);
    }

    public void setEffectiveTo(UUID id, UUID teamId, LocalDate effectiveTo, UUID updatedBy) {
        LocalDateTime now = LocalDateTime.now(clock);
        dsl.update(TABLE)
                .set(EFFECTIVE_TO, effectiveTo)
                .set(UPDATED_AT, now)
                .set(UPDATED_BY, updatedBy)
                .where(ID.eq(id)
                        .and(TEAM_ID.eq(teamId))
                        .and(DELETED_AT.isNull()))
                .execute();
    }

    public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
        LocalDateTime now = LocalDateTime.now(clock);
        dsl.update(TABLE)
                .set(DELETED_AT, now)
                .where(ID.eq(id)
                        .and(TEAM_ID.eq(teamId)))
                .execute();
    }

    private ContractPaymentInstruction toDomain(Record record) {
        ContractPaymentInstruction cpi = new ContractPaymentInstruction();
        cpi.setId(record.get(ID));
        cpi.setIdentifier(record.get(IDENTIFIER));
        cpi.setTeamId(record.get(TEAM_ID));
        cpi.setContractId(record.get(CONTRACT_ID));
        cpi.setPaymentInstructionId(record.get(PAYMENT_INSTRUCTION_ID));
        cpi.setIsCustom(record.get(IS_CUSTOM));
        cpi.setCustomName(record.get(CUSTOM_NAME));
        cpi.setCustomDescription(record.get(CUSTOM_DESCRIPTION));
        cpi.setCustomPaymentMethod(record.get(CUSTOM_PAYMENT_METHOD));
        cpi.setCustomBankName(record.get(CUSTOM_BANK_NAME));
        cpi.setCustomAccountHolderName(record.get(CUSTOM_ACCOUNT_HOLDER_NAME));
        cpi.setCustomIban(record.get(CUSTOM_IBAN));
        cpi.setCustomBicSwift(record.get(CUSTOM_BIC_SWIFT));
        cpi.setCustomAccountNumber(record.get(CUSTOM_ACCOUNT_NUMBER));
        cpi.setCustomRoutingNumber(record.get(CUSTOM_ROUTING_NUMBER));
        cpi.setCustomPaymentReference(record.get(CUSTOM_PAYMENT_REFERENCE));
        cpi.setCustomAdditionalDetails(record.get(CUSTOM_ADDITIONAL_DETAILS));
        cpi.setEffectiveFrom(toLocalDate(record.get("effective_from")));
        cpi.setEffectiveTo(toLocalDate(record.get("effective_to")));
        cpi.setNotes(record.get(NOTES));
        cpi.setCreatedAt(toInstant(record.get("created_at")));
        cpi.setUpdatedAt(toInstant(record.get("updated_at")));
        cpi.setCreatedBy(record.get(CREATED_BY));
        cpi.setUpdatedBy(record.get(UPDATED_BY));
        cpi.setDeletedAt(toInstant(record.get("deleted_at")));
        return cpi;
    }

    private static LocalDate toLocalDate(Object val) {
        if (val instanceof LocalDate ld) return ld;
        if (val instanceof java.sql.Date sd) return sd.toLocalDate();
        return null;
    }

    private static Instant toInstant(Object val) {
        if (val instanceof LocalDateTime ldt) return ldt.toInstant(UTC);
        if (val instanceof java.sql.Timestamp ts) return ts.toInstant();
        return null;
    }
}
