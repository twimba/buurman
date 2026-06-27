package com.buurman.service;

import static com.buurman.jooq.generated.Tables.CONTRACTS;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.jooq.Field;
import org.jooq.impl.DSL;

import com.buurman.domain.ContractExtension;

/**
 * Shared helper for computing effective end date from contract extensions. Provides both a JOOQ
 * {@link Field} for SQL queries and a Java method for in-memory computation.
 *
 * <p>The effective end date is the new_end_date from the latest ACTIVE extension, falling back to
 * the contract's original end_date.
 */
public final class EffectiveEndDateHelper {

  private static final org.jooq.Table<?> CE = DSL.table("contract_extensions");
  private static final Field<java.util.UUID> CE_CONTRACT_ID =
      DSL.field("contract_extensions.contract_id", java.util.UUID.class);
  private static final Field<String> CE_STATUS =
      DSL.field("contract_extensions.status", String.class);
  private static final Field<java.time.LocalDateTime> CE_DELETED_AT =
      DSL.field("contract_extensions.deleted_at", java.time.LocalDateTime.class);
  private static final Field<Integer> CE_EXTENSION_NUMBER =
      DSL.field("contract_extensions.extension_number", Integer.class);
  private static final Field<LocalDate> CE_NEW_END_DATE =
      DSL.field("contract_extensions.new_end_date", LocalDate.class);

  private EffectiveEndDateHelper() {}

  /**
   * JOOQ field expression that computes the effective end date for use in SQL queries. Uses CASE
   * WHEN EXISTS to properly handle rollover-to-indefinite (new_end_date = NULL means indefinite).
   *
   * <p>When an ACTIVE extension exists with NULL new_end_date, returns NULL (indefinite). When an
   * ACTIVE extension exists with a date, returns that date. Otherwise falls back to
   * contracts.end_date.
   *
   * <p>Usage: {@code select(EffectiveEndDateHelper.effectiveEndDate()).from(CONTRACTS)}
   */
  public static Field<LocalDate> effectiveEndDate() {
    return effectiveEndDateExpr().as("effective_end_date");
  }

  /**
   * Same expression as {@link #effectiveEndDate()} but WITHOUT the {@code effective_end_date}
   * alias.
   *
   * <p>Use this in WHERE clauses: PostgreSQL does not allow SELECT-list aliases to be referenced in
   * WHERE, so the aliased variant renders as a bare {@code "effective_end_date"} reference and
   * fails with "column does not exist". The aliased variant is only safe in SELECT and ORDER BY.
   */
  public static Field<LocalDate> effectiveEndDateExpr() {
    return DSL.when(
            DSL.exists(
                DSL.selectOne()
                    .from(CE)
                    .where(CE_CONTRACT_ID.eq(CONTRACTS.ID))
                    .and(CE_STATUS.eq("ACTIVE"))
                    .and(CE_DELETED_AT.isNull())),
            DSL.field(
                DSL.select(CE_NEW_END_DATE)
                    .from(CE)
                    .where(CE_CONTRACT_ID.eq(CONTRACTS.ID))
                    .and(CE_STATUS.eq("ACTIVE"))
                    .and(CE_DELETED_AT.isNull())
                    .orderBy(CE_EXTENSION_NUMBER.desc())
                    .limit(1)))
        .otherwise(CONTRACTS.END_DATE);
  }

  /**
   * Java helper for computing effective end date from already-loaded entities.
   *
   * @param contractEndDate the contract's original end date
   * @param extensions all extensions for the contract (any status)
   * @return the effective end date, or empty if the contract is indefinite and no extension changes
   *     that
   */
  public static Optional<LocalDate> computeEffectiveEndDate(
      Optional<LocalDate> contractEndDate, List<ContractExtension> extensions) {
    return extensions.stream()
        .filter(e -> e.getStatus() == ContractExtension.ExtensionStatus.ACTIVE)
        .filter(e -> e.getDeletedAt().isEmpty())
        .max(java.util.Comparator.comparingInt(ContractExtension::getExtensionNumber))
        .flatMap(ContractExtension::getNewEndDate)
        .or(() -> contractEndDate);
  }
}
