package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.UNITS;

import org.jooq.Condition;

/**
 * The one definition of "the unit set that counts": a unit row that is itself not soft-deleted and
 * whose parent property is not soft-deleted either. Before this class existed, {@link
 * UnitRepository}, {@link PropertyRepository}, {@code BackofficeTeamStatsRepository} and {@code
 * DatabaseMetricsRepository} each re-derived this predicate by hand ({@code
 * UNITS.DELETED_AT.isNull()} + {@code PROPERTIES.DELETED_AT.isNull()}, assembled separately every
 * time), and agreed with each other only by luck -- a fix made two of them agree after they had
 * already silently diverged on {@code vacantUnitCount} (BUUR-106 wave3c Important 6).
 *
 * <p>Every rollup that counts or groups units should compose this condition instead of re-deriving
 * it. Callers must join {@code UNITS} to {@code PROPERTIES} themselves (typically {@code
 * .on(PROPERTIES.ID.eq(UNITS.PROPERTY_ID))}) before adding this condition -- it references both
 * tables and does not perform the join itself.
 */
public final class UnitScope {

  private UnitScope() {}

  /** Unit not soft-deleted, and its parent property not soft-deleted either. */
  public static Condition active() {
    return UNITS.DELETED_AT.isNull().and(PROPERTIES.DELETED_AT.isNull());
  }
}
