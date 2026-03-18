package com.buurman.repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class JurisdictionDefaultRepository {

  private final DSLContext dsl;
  private final Clock clock;

  private static final Table<?> JD = table("jurisdiction_defaults");
  private static final Field<UUID> ID = field("id", UUID.class);
  private static final Field<String> COUNTRY_CODE = field("country_code", String.class);
  private static final Field<String> REGION_CODE = field("region_code", String.class);
  private static final Field<String> LANDLORD_TYPE = field("landlord_type", String.class);
  private static final Field<Boolean> FURNISHED = field("furnished", Boolean.class);
  private static final Field<String> FIELD_NAME = field("field_name", String.class);
  private static final Field<String> VALUE = field("value", String.class);
  private static final Field<Date> VALID_FROM = field("valid_from", Date.class);
  private static final Field<Date> VALID_UNTIL = field("valid_until", Date.class);
  private static final Field<String> NOTES = field("notes", String.class);

  /**
   * Cascading lookup: finds the most specific defaults matching the given criteria. Returns a
   * Map&lt;fieldName, value&gt; with most-specific values winning.
   *
   * <p>Cascade order (most to least specific):
   *
   * <ol>
   *   <li>country + region + landlordType + furnished
   *   <li>country + region + landlordType
   *   <li>country + region
   *   <li>country + landlordType + furnished
   *   <li>country + landlordType
   *   <li>country (least specific)
   * </ol>
   */
  public Map<String, String> findDefaults(
      String countryCode,
      Optional<String> regionCode,
      Optional<String> landlordType,
      Optional<Boolean> furnished) {
    LocalDate today = LocalDate.now(clock);

    Condition baseCondition =
        COUNTRY_CODE
            .eq(countryCode)
            .and(VALID_FROM.le(Date.valueOf(today)))
            .and(VALID_UNTIL.isNull().or(VALID_UNTIL.gt(Date.valueOf(today))));

    // Start with least-specific and overlay more-specific
    Map<String, String> result = new LinkedHashMap<>();

    // Level 6: country only
    overlayDefaults(
        result,
        baseCondition
            .and(REGION_CODE.isNull())
            .and(LANDLORD_TYPE.isNull())
            .and(FURNISHED.isNull()));

    // Level 5: country + landlordType
    landlordType.ifPresent(
        lt ->
            overlayDefaults(
                result,
                baseCondition
                    .and(REGION_CODE.isNull())
                    .and(LANDLORD_TYPE.eq(lt))
                    .and(FURNISHED.isNull())));

    // Level 4: country + landlordType + furnished
    if (landlordType.isPresent() && furnished.isPresent()) {
      overlayDefaults(
          result,
          baseCondition
              .and(REGION_CODE.isNull())
              .and(LANDLORD_TYPE.eq(landlordType.get()))
              .and(FURNISHED.eq(furnished.get())));
    }

    // Level 3: country + region
    regionCode.ifPresent(
        rc ->
            overlayDefaults(
                result,
                baseCondition
                    .and(REGION_CODE.eq(rc))
                    .and(LANDLORD_TYPE.isNull())
                    .and(FURNISHED.isNull())));

    // Level 2: country + region + landlordType
    if (regionCode.isPresent() && landlordType.isPresent()) {
      overlayDefaults(
          result,
          baseCondition
              .and(REGION_CODE.eq(regionCode.get()))
              .and(LANDLORD_TYPE.eq(landlordType.get()))
              .and(FURNISHED.isNull()));
    }

    // Level 1: country + region + landlordType + furnished (most specific)
    if (regionCode.isPresent() && landlordType.isPresent() && furnished.isPresent()) {
      overlayDefaults(
          result,
          baseCondition
              .and(REGION_CODE.eq(regionCode.get()))
              .and(LANDLORD_TYPE.eq(landlordType.get()))
              .and(FURNISHED.eq(furnished.get())));
    }

    return result;
  }

  private void overlayDefaults(Map<String, String> result, Condition condition) {
    dsl.select(FIELD_NAME, VALUE)
        .from(JD)
        .where(condition)
        .fetch()
        .forEach(record -> result.put(record.get(FIELD_NAME), record.get(VALUE)));
  }
}
