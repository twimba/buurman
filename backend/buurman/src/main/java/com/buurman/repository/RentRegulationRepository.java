package com.buurman.repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.springframework.stereotype.Repository;

import com.buurman.domain.MaxIncreaseType;
import com.buurman.domain.RentFrequency;
import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.RentRegulationRegion;
import com.buurman.domain.RentRegulationRule;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RentRegulationRepository {

  private final DSLContext dsl;
  private final Clock clock;

  // --- Countries table ---
  private static final Table<?> COUNTRIES = table("rent_regulation_countries");
  private static final Field<UUID> C_ID = field("id", UUID.class);
  private static final Field<String> C_IDENTIFIER = field("identifier", String.class);
  private static final Field<String> C_COUNTRY_CODE = field("country_code", String.class);
  private static final Field<String> C_COUNTRY_NAME = field("country_name", String.class);
  private static final Field<Boolean> C_HAS_REGIONAL =
      field("has_regional_regulations", Boolean.class);
  private static final Field<String> C_SUMMARY = field("summary", String.class);
  private static final Field<Timestamp> C_LAST_REVIEWED_AT =
      field("last_reviewed_at", Timestamp.class);
  private static final Field<Timestamp> C_CREATED_AT = field("created_at", Timestamp.class);
  private static final Field<Timestamp> C_UPDATED_AT = field("updated_at", Timestamp.class);
  private static final Field<String> C_CREATED_BY = field("created_by", String.class);
  private static final Field<String> C_UPDATED_BY = field("updated_by", String.class);

  // --- Regions table ---
  private static final Table<?> REGIONS = table("rent_regulation_regions");
  private static final Field<UUID> R_ID = field("id", UUID.class);
  private static final Field<String> R_IDENTIFIER = field("identifier", String.class);
  private static final Field<UUID> R_COUNTRY_ID = field("country_id", UUID.class);
  private static final Field<String> R_REGION_CODE = field("region_code", String.class);
  private static final Field<String> R_REGION_NAME = field("region_name", String.class);
  private static final Field<String> R_SUMMARY = field("summary", String.class);
  private static final Field<Timestamp> R_CREATED_AT = field("created_at", Timestamp.class);
  private static final Field<Timestamp> R_UPDATED_AT = field("updated_at", Timestamp.class);
  private static final Field<String> R_CREATED_BY = field("created_by", String.class);
  private static final Field<String> R_UPDATED_BY = field("updated_by", String.class);

  // --- Rules table ---
  private static final Table<?> RULES = table("rent_regulation_rules");
  private static final Field<UUID> RL_ID = field("id", UUID.class);
  private static final Field<String> RL_IDENTIFIER = field("identifier", String.class);
  private static final Field<UUID> RL_COUNTRY_ID = field("country_id", UUID.class);
  private static final Field<UUID> RL_REGION_ID = field("region_id", UUID.class);
  private static final Field<Integer> RL_YEAR = field("year", Integer.class);
  private static final Field<String> RL_PROPERTY_CATEGORY =
      field("property_category", String.class);
  private static final Field<String> RL_SECTOR = field("sector", String.class);
  private static final Field<BigDecimal> RL_MAX_INCREASE_PERCENTAGE =
      field("max_increase_percentage", BigDecimal.class);
  private static final Field<String> RL_MAX_INCREASE_TYPE =
      field("max_increase_type", String.class);
  private static final Field<String> RL_INDEX_NAME = field("index_name", String.class);
  private static final Field<BigDecimal> RL_INDEX_VALUE = field("index_value", BigDecimal.class);
  private static final Field<Date> RL_EFFECTIVE_DATE = field("effective_date", Date.class);
  private static final Field<Integer> RL_NOTICE_PERIOD_DAYS =
      field("notice_period_days", Integer.class);
  private static final Field<String> RL_FREQUENCY = field("frequency", String.class);
  private static final Field<String> RL_ADDITIONAL_CONDITIONS =
      field("additional_conditions", String.class);
  private static final Field<String> RL_SOURCE_URL = field("source_url", String.class);
  private static final Field<String> RL_NOTES = field("notes", String.class);
  private static final Field<Timestamp> RL_CREATED_AT = field("created_at", Timestamp.class);
  private static final Field<Timestamp> RL_UPDATED_AT = field("updated_at", Timestamp.class);
  private static final Field<String> RL_CREATED_BY = field("created_by", String.class);
  private static final Field<String> RL_UPDATED_BY = field("updated_by", String.class);

  // ==================== Country operations ====================

  public List<RentRegulationCountry> findAllCountries() {
    return List.copyOf(
        dsl.select().from(COUNTRIES).orderBy(C_COUNTRY_NAME.asc()).fetch(this::toCountryDomain));
  }

  public Optional<RentRegulationCountry> findCountryByCode(String countryCode) {
    return dsl.select()
        .from(COUNTRIES)
        .where(C_COUNTRY_CODE.eq(countryCode))
        .fetchOptional(this::toCountryDomain);
  }

  public Optional<RentRegulationCountry> findCountryById(UUID id) {
    return dsl.select().from(COUNTRIES).where(C_ID.eq(id)).fetchOptional(this::toCountryDomain);
  }

  public RentRegulationCountry saveCountry(RentRegulationCountry country) {
    Timestamp now = Timestamp.from(clock.instant());

    if (country.getId() == null) {
      UUID id = UUID.randomUUID();
      dsl.insertInto(COUNTRIES)
          .set(C_ID, id)
          .set(C_IDENTIFIER, country.getIdentifier().orElseThrow().value())
          .set(C_COUNTRY_CODE, country.getCountryCode())
          .set(C_COUNTRY_NAME, country.getCountryName())
          .set(C_HAS_REGIONAL, country.isHasRegionalRegulations())
          .set(C_SUMMARY, country.getSummary().orElse(null))
          .set(C_LAST_REVIEWED_AT, country.getLastReviewedAt().map(Timestamp::from).orElse(null))
          .set(C_CREATED_AT, now)
          .set(C_UPDATED_AT, now)
          .set(C_CREATED_BY, country.getCreatedBy().orElse(null))
          .set(C_UPDATED_BY, country.getUpdatedBy().orElse(null))
          .execute();

      country.setId(id);
      country.setCreatedAt(now.toInstant());
      country.setUpdatedAt(now.toInstant());
    } else {
      dsl.update(COUNTRIES)
          .set(C_COUNTRY_CODE, country.getCountryCode())
          .set(C_COUNTRY_NAME, country.getCountryName())
          .set(C_HAS_REGIONAL, country.isHasRegionalRegulations())
          .set(C_SUMMARY, country.getSummary().orElse(null))
          .set(C_LAST_REVIEWED_AT, country.getLastReviewedAt().map(Timestamp::from).orElse(null))
          .set(C_UPDATED_AT, now)
          .set(C_UPDATED_BY, country.getUpdatedBy().orElse(null))
          .where(C_ID.eq(country.getId()))
          .execute();

      country.setUpdatedAt(now.toInstant());
    }
    return country;
  }

  public RentRegulationCountry getCountryByCode(String countryCode) {
    return findCountryByCode(countryCode)
        .orElseThrow(
            () -> new NotFoundException("Rent regulation country not found: " + countryCode));
  }

  public RentRegulationCountry updateCountry(RentRegulationCountry country) {
    return saveCountry(country);
  }

  public void deleteCountry(UUID id) {
    dsl.deleteFrom(COUNTRIES).where(C_ID.eq(id)).execute();
  }

  // ==================== Region operations ====================

  public List<RentRegulationRegion> findRegionsByCountryId(UUID countryId) {
    return List.copyOf(
        dsl.select()
            .from(REGIONS)
            .where(R_COUNTRY_ID.eq(countryId))
            .orderBy(R_REGION_NAME.asc())
            .fetch(this::toRegionDomain));
  }

  public Optional<RentRegulationRegion> findRegionByCode(UUID countryId, String regionCode) {
    return dsl.select()
        .from(REGIONS)
        .where(R_COUNTRY_ID.eq(countryId).and(R_REGION_CODE.eq(regionCode)))
        .fetchOptional(this::toRegionDomain);
  }

  public RentRegulationRegion saveRegion(RentRegulationRegion region) {
    Timestamp now = Timestamp.from(clock.instant());

    if (region.getId() == null) {
      UUID id = UUID.randomUUID();
      dsl.insertInto(REGIONS)
          .set(R_ID, id)
          .set(R_IDENTIFIER, region.getIdentifier().orElseThrow().value())
          .set(R_COUNTRY_ID, region.getCountryId())
          .set(R_REGION_CODE, region.getRegionCode())
          .set(R_REGION_NAME, region.getRegionName())
          .set(R_SUMMARY, region.getSummary().orElse(null))
          .set(R_CREATED_AT, now)
          .set(R_UPDATED_AT, now)
          .set(R_CREATED_BY, region.getCreatedBy().orElse(null))
          .set(R_UPDATED_BY, region.getUpdatedBy().orElse(null))
          .execute();

      region.setId(id);
      region.setCreatedAt(now.toInstant());
      region.setUpdatedAt(now.toInstant());
    } else {
      dsl.update(REGIONS)
          .set(R_REGION_CODE, region.getRegionCode())
          .set(R_REGION_NAME, region.getRegionName())
          .set(R_SUMMARY, region.getSummary().orElse(null))
          .set(R_UPDATED_AT, now)
          .set(R_UPDATED_BY, region.getUpdatedBy().orElse(null))
          .where(R_ID.eq(region.getId()))
          .execute();

      region.setUpdatedAt(now.toInstant());
    }
    return region;
  }

  public RentRegulationRegion getRegionByCountryIdAndCode(UUID countryId, String regionCode) {
    return findRegionByCode(countryId, regionCode)
        .orElseThrow(
            () -> new NotFoundException("Rent regulation region not found: " + regionCode));
  }

  public RentRegulationRegion updateRegion(RentRegulationRegion region) {
    return saveRegion(region);
  }

  public void deleteRegion(UUID id) {
    dsl.deleteFrom(REGIONS).where(R_ID.eq(id)).execute();
  }

  // ==================== Rule operations ====================

  public List<RentRegulationRule> findRulesByCountryId(UUID countryId) {
    return List.copyOf(
        dsl.select()
            .from(RULES)
            .where(RL_COUNTRY_ID.eq(countryId))
            .orderBy(RL_YEAR.desc(), RL_PROPERTY_CATEGORY.asc())
            .fetch(this::toRuleDomain));
  }

  public List<RentRegulationRule> findRulesByCountryIdAndYear(UUID countryId, int year) {
    return List.copyOf(
        dsl.select()
            .from(RULES)
            .where(RL_COUNTRY_ID.eq(countryId).and(RL_YEAR.eq(year)))
            .orderBy(RL_PROPERTY_CATEGORY.asc())
            .fetch(this::toRuleDomain));
  }

  public List<RentRegulationRule> findRulesByRegionIdAndYear(UUID regionId, int year) {
    return List.copyOf(
        dsl.select()
            .from(RULES)
            .where(RL_REGION_ID.eq(regionId).and(RL_YEAR.eq(year)))
            .orderBy(RL_PROPERTY_CATEGORY.asc())
            .fetch(this::toRuleDomain));
  }

  public List<RentRegulationRule> findCurrentRules(UUID countryId, Optional<UUID> regionId) {
    int currentYear = LocalDate.now(clock).getYear();
    if (regionId.isPresent()) {
      return findRulesByRegionIdAndYear(regionId.get(), currentYear);
    }
    return findRulesByCountryIdAndYear(countryId, currentYear);
  }

  public Optional<RentRegulationRule> findRuleByIdentifier(Sid identifier) {
    return dsl.select()
        .from(RULES)
        .where(RL_IDENTIFIER.eq(identifier.value()))
        .fetchOptional(this::toRuleDomain);
  }

  public RentRegulationRule saveRule(RentRegulationRule rule) {
    Timestamp now = Timestamp.from(clock.instant());

    if (rule.getId() == null) {
      UUID id = UUID.randomUUID();
      dsl.insertInto(RULES)
          .set(RL_ID, id)
          .set(RL_IDENTIFIER, rule.getIdentifier().orElseThrow().value())
          .set(RL_COUNTRY_ID, rule.getCountryId())
          .set(RL_REGION_ID, rule.getRegionId().orElse(null))
          .set(RL_YEAR, rule.getYear())
          .set(RL_PROPERTY_CATEGORY, rule.getPropertyCategory())
          .set(RL_SECTOR, rule.getSector().orElse(null))
          .set(RL_MAX_INCREASE_PERCENTAGE, rule.getMaxIncreasePercentage().orElse(null))
          .set(RL_MAX_INCREASE_TYPE, rule.getMaxIncreaseType().name())
          .set(RL_INDEX_NAME, rule.getIndexName().orElse(null))
          .set(RL_INDEX_VALUE, rule.getIndexValue().orElse(null))
          .set(RL_EFFECTIVE_DATE, rule.getEffectiveDate().map(Date::valueOf).orElse(null))
          .set(RL_NOTICE_PERIOD_DAYS, rule.getNoticePeriodDays().orElse(null))
          .set(RL_FREQUENCY, rule.getFrequency().name())
          .set(RL_ADDITIONAL_CONDITIONS, rule.getAdditionalConditions().orElse(null))
          .set(RL_SOURCE_URL, rule.getSourceUrl().orElse(null))
          .set(RL_NOTES, rule.getNotes().orElse(null))
          .set(RL_CREATED_AT, now)
          .set(RL_UPDATED_AT, now)
          .set(RL_CREATED_BY, rule.getCreatedBy().orElse(null))
          .set(RL_UPDATED_BY, rule.getUpdatedBy().orElse(null))
          .execute();

      rule.setId(id);
      rule.setCreatedAt(now.toInstant());
      rule.setUpdatedAt(now.toInstant());
    } else {
      dsl.update(RULES)
          .set(RL_COUNTRY_ID, rule.getCountryId())
          .set(RL_REGION_ID, rule.getRegionId().orElse(null))
          .set(RL_YEAR, rule.getYear())
          .set(RL_PROPERTY_CATEGORY, rule.getPropertyCategory())
          .set(RL_SECTOR, rule.getSector().orElse(null))
          .set(RL_MAX_INCREASE_PERCENTAGE, rule.getMaxIncreasePercentage().orElse(null))
          .set(RL_MAX_INCREASE_TYPE, rule.getMaxIncreaseType().name())
          .set(RL_INDEX_NAME, rule.getIndexName().orElse(null))
          .set(RL_INDEX_VALUE, rule.getIndexValue().orElse(null))
          .set(RL_EFFECTIVE_DATE, rule.getEffectiveDate().map(Date::valueOf).orElse(null))
          .set(RL_NOTICE_PERIOD_DAYS, rule.getNoticePeriodDays().orElse(null))
          .set(RL_FREQUENCY, rule.getFrequency().name())
          .set(RL_ADDITIONAL_CONDITIONS, rule.getAdditionalConditions().orElse(null))
          .set(RL_SOURCE_URL, rule.getSourceUrl().orElse(null))
          .set(RL_NOTES, rule.getNotes().orElse(null))
          .set(RL_UPDATED_AT, now)
          .set(RL_UPDATED_BY, rule.getUpdatedBy().orElse(null))
          .where(RL_ID.eq(rule.getId()))
          .execute();

      rule.setUpdatedAt(now.toInstant());
    }
    return rule;
  }

  public RentRegulationRule getRuleByIdentifier(Sid identifier) {
    return findRuleByIdentifier(identifier)
        .orElseThrow(
            () -> new NotFoundException("Rent regulation rule not found: " + identifier.value()));
  }

  public RentRegulationRule updateRule(RentRegulationRule rule) {
    return saveRule(rule);
  }

  public void deleteRule(UUID id) {
    dsl.deleteFrom(RULES).where(RL_ID.eq(id)).execute();
  }

  public void saveRulesBulk(List<RentRegulationRule> rules) {
    for (RentRegulationRule rule : rules) {
      saveRule(rule);
    }
  }

  // ==================== Mappers ====================

  private RentRegulationCountry toCountryDomain(Record record) {
    RentRegulationCountry country = new RentRegulationCountry();
    country.setId(record.get(C_ID));
    country.setIdentifier(Optional.of(Sid.of(record.get(C_IDENTIFIER))));
    country.setCountryCode(record.get(C_COUNTRY_CODE));
    country.setCountryName(record.get(C_COUNTRY_NAME));
    Boolean hasRegional = record.get(C_HAS_REGIONAL);
    country.setHasRegionalRegulations(hasRegional != null && hasRegional);
    country.setSummary(Optional.ofNullable(record.get(C_SUMMARY)));
    Timestamp lastReviewed = record.get(C_LAST_REVIEWED_AT);
    country.setLastReviewedAt(Optional.ofNullable(lastReviewed).map(Timestamp::toInstant));
    Timestamp createdAt = record.get(C_CREATED_AT);
    if (createdAt != null) {
      country.setCreatedAt(createdAt.toInstant());
    }
    Timestamp updatedAt = record.get(C_UPDATED_AT);
    if (updatedAt != null) {
      country.setUpdatedAt(updatedAt.toInstant());
    }
    country.setCreatedBy(Optional.ofNullable(record.get(C_CREATED_BY)));
    country.setUpdatedBy(Optional.ofNullable(record.get(C_UPDATED_BY)));
    return country;
  }

  private RentRegulationRegion toRegionDomain(Record record) {
    RentRegulationRegion region = new RentRegulationRegion();
    region.setId(record.get(R_ID));
    region.setIdentifier(Optional.of(Sid.of(record.get(R_IDENTIFIER))));
    region.setCountryId(record.get(R_COUNTRY_ID));
    region.setRegionCode(record.get(R_REGION_CODE));
    region.setRegionName(record.get(R_REGION_NAME));
    region.setSummary(Optional.ofNullable(record.get(R_SUMMARY)));
    Timestamp createdAt = record.get(R_CREATED_AT);
    if (createdAt != null) {
      region.setCreatedAt(createdAt.toInstant());
    }
    Timestamp updatedAt = record.get(R_UPDATED_AT);
    if (updatedAt != null) {
      region.setUpdatedAt(updatedAt.toInstant());
    }
    region.setCreatedBy(Optional.ofNullable(record.get(R_CREATED_BY)));
    region.setUpdatedBy(Optional.ofNullable(record.get(R_UPDATED_BY)));
    return region;
  }

  private RentRegulationRule toRuleDomain(Record record) {
    RentRegulationRule rule = new RentRegulationRule();
    rule.setId(record.get(RL_ID));
    rule.setIdentifier(Optional.of(Sid.of(record.get(RL_IDENTIFIER))));
    rule.setCountryId(record.get(RL_COUNTRY_ID));
    rule.setRegionId(Optional.ofNullable(record.get(RL_REGION_ID)));
    rule.setYear(record.get(RL_YEAR));
    rule.setPropertyCategory(record.get(RL_PROPERTY_CATEGORY));
    rule.setSector(Optional.ofNullable(record.get(RL_SECTOR)));
    rule.setMaxIncreasePercentage(Optional.ofNullable(record.get(RL_MAX_INCREASE_PERCENTAGE)));
    rule.setMaxIncreaseType(MaxIncreaseType.valueOf(record.get(RL_MAX_INCREASE_TYPE)));
    rule.setIndexName(Optional.ofNullable(record.get(RL_INDEX_NAME)));
    rule.setIndexValue(Optional.ofNullable(record.get(RL_INDEX_VALUE)));
    Date effectiveDate = record.get(RL_EFFECTIVE_DATE);
    rule.setEffectiveDate(Optional.ofNullable(effectiveDate).map(Date::toLocalDate));
    rule.setNoticePeriodDays(Optional.ofNullable(record.get(RL_NOTICE_PERIOD_DAYS)));
    rule.setFrequency(RentFrequency.valueOf(record.get(RL_FREQUENCY)));
    rule.setAdditionalConditions(Optional.ofNullable(record.get(RL_ADDITIONAL_CONDITIONS)));
    rule.setSourceUrl(Optional.ofNullable(record.get(RL_SOURCE_URL)));
    rule.setNotes(Optional.ofNullable(record.get(RL_NOTES)));
    Timestamp createdAt = record.get(RL_CREATED_AT);
    if (createdAt != null) {
      rule.setCreatedAt(createdAt.toInstant());
    }
    Timestamp updatedAt = record.get(RL_UPDATED_AT);
    if (updatedAt != null) {
      rule.setUpdatedAt(updatedAt.toInstant());
    }
    rule.setCreatedBy(Optional.ofNullable(record.get(RL_CREATED_BY)));
    rule.setUpdatedBy(Optional.ofNullable(record.get(RL_UPDATED_BY)));
    return rule;
  }
}
