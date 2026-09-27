package com.buurman.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.jooq.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.jooq.generated.Tables;

/**
 * BUUR-106 wave3c Critical 5: V068 dropped ten-plus columns from {@code properties} and two whole
 * tables ({@code unit_residential_details}, {@code unit_amenities}) from underneath {@link
 * TakeoutService} without anyone noticing, because nothing asserted the export's table coverage
 * against the JOOQ meta-model. A landlord exercising GDPR Art. 20 data portability got an export
 * with no dwelling data at all.
 *
 * <p>This walks every table constant on the generated {@link Tables} class, keeps the ones that
 * carry a {@code team_id} column (the working definition of "this team's data"), and asserts each
 * one is either exported ({@link TakeoutService#EXPORTED_TABLES}) or explicitly named in {@link
 * #KNOWN_GAPS} below. The next migration that adds a team-scoped table now fails this test instead
 * of silently shipping a hole in the export.
 */
@DisplayName("TakeoutService table coverage vs. the JOOQ meta-model")
class TakeoutMetaModelCoverageTest {

  /**
   * Team-scoped tables not covered by {@link TakeoutService#EXPORTED_TABLES}, as of BUUR-106 wave3c
   * (2026-09-27). None of these were touched by this fix -- they predate it and are pre-existing
   * gaps, tracked here instead of silently passing so they stay visible. Closing one means removing
   * it from this set AND adding its table to {@code TakeoutService.EXPORTED_TABLES} (and the {@code
   * categories} array / {@code TakeoutGoogleSheetService.TABS}); this test enforces that pairing
   * from here on.
   */
  private static final Set<String> KNOWN_GAPS =
      Set.of(
          "AUDIT_LOG",
          "BROADCAST_MESSAGE_TEAMS",
          "CALENDAR_FEEDS",
          "CONTACT_CREDITS",
          "CONTACT_NOTES",
          "CONTACT_RELATIONSHIPS",
          "CONTACT_TAGS",
          "CONTRACT_EXTENSIONS",
          "CONTRACT_RENT_COMPONENTS",
          "CURRENCY_CHANGE_LOG",
          "DATA_IMPORTS",
          "DATA_TAKEOUTS",
          "DEPOSIT_DEDUCTIONS",
          "DEPOSITS",
          "FEATURE_FLAG_OVERRIDES",
          "FINANCING_PAYMENTS",
          "GENERATED_REPORTS",
          "IMPERSONATION_SESSIONS",
          "NOTIFICATION_OUTBOX",
          "NOTIFICATIONS",
          "PAYMENT_INSTRUCTIONS",
          "PAYMENT_PLANS",
          "PAYMENT_REMINDERS",
          "PROPERTY_AGRICULTURAL_DETAILS",
          "PROPERTY_COMMERCIAL_DETAILS",
          "PROPERTY_CONTACT_HISTORY",
          "PROPERTY_INDUSTRIAL_DETAILS",
          "RENT_REGULATION_COUNTRY_REQUESTS",
          "TEAM_INVITATIONS",
          "TEAM_PREFERENCES",
          "USERS",
          "USER_TEAM_NOTIFICATION_PREFERENCES",
          "WWS_CALCULATIONS");

  @Test
  @DisplayName("every team-scoped table is exported or explicitly named as a known gap")
  void everyTeamScopedTableIsExportedOrAKnownGap() throws IllegalAccessException {
    Set<String> exportedNames = new HashSet<>();
    for (Table<?> table : TakeoutService.EXPORTED_TABLES) {
      exportedNames.add(table.getName().toUpperCase(Locale.ROOT));
    }

    Set<String> uncovered = new TreeSet<>();
    for (Field field : Tables.class.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers())
          || !Table.class.isAssignableFrom(field.getType())) {
        continue;
      }
      Table<?> table = (Table<?>) field.get(null);
      if (table.field("team_id", UUID.class) == null) {
        continue; // not team-scoped -- e.g. global catalogues like amenities
      }
      String name = table.getName().toUpperCase(Locale.ROOT);
      if (!exportedNames.contains(name) && !KNOWN_GAPS.contains(name)) {
        uncovered.add(name);
      }
    }

    assertThat(uncovered)
        .as(
            "team-scoped table(s) neither in TakeoutService.EXPORTED_TABLES nor in"
                + " TakeoutMetaModelCoverageTest.KNOWN_GAPS -- export it, or if deliberately"
                + " excluded, add it to KNOWN_GAPS with a reason")
        .isEmpty();
  }

  @Test
  @DisplayName("the four dwelling/allocation tables this fix added are exported")
  void addedTablesAreExported() {
    Set<String> exportedNames = new HashSet<>();
    for (Table<?> table : TakeoutService.EXPORTED_TABLES) {
      exportedNames.add(table.getName().toUpperCase(Locale.ROOT));
    }

    assertThat(exportedNames)
        .contains("UNITS", "UNIT_RESIDENTIAL_DETAILS", "UNIT_AMENITIES", "EXPENSE_ALLOCATIONS");
  }
}
