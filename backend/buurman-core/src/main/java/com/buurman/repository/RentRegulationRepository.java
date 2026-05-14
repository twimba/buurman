package com.buurman.repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
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
import com.buurman.dto.response.CountryRegulationRequestSummary;
import com.buurman.dto.response.CountryRegulationRequester;
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
  // BUUR-93 dimensional columns (V055)
  private static final Field<String> RL_REGIME = field("regime", String.class);
  private static final Field<String> RL_PROPERTY_TYPE = field("property_type", String.class);
  private static final Field<String> RL_CONTRACT_TYPE = field("contract_type", String.class);
  private static final Field<String> RL_TAX_REGIME = field("tax_regime", String.class);
  private static final Field<String> RL_TENANCY_PHASE = field("tenancy_phase", String.class);
  private static final Field<Integer> RL_BUILD_YEAR_MIN = field("build_year_min", Integer.class);
  private static final Field<Integer> RL_BUILD_YEAR_MAX = field("build_year_max", Integer.class);
  private static final Field<String> RL_EPC_CLASS_MIN = field("epc_class_min", String.class);
  private static final Field<String> RL_EPC_CLASS_MAX = field("epc_class_max", String.class);
  private static final Field<Date> RL_CONTRACT_SIGNED_AFTER =
      field("contract_signed_after", Date.class);
  private static final Field<Date> RL_CONTRACT_SIGNED_BEFORE =
      field("contract_signed_before", Date.class);
  private static final Field<Integer> RL_LANDLORD_MIN_PROPERTIES =
      field("landlord_min_properties", Integer.class);
  private static final Field<String> RL_AREA_CODE = field("area_code", String.class);
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

  public List<RentRegulationRule> findNationalRulesByCountryIdAndYear(UUID countryId, int year) {
    return List.copyOf(
        dsl.select()
            .from(RULES)
            .where(RL_COUNTRY_ID.eq(countryId).and(RL_YEAR.eq(year)).and(RL_REGION_ID.isNull()))
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
    return regionId
        .map(uuid -> findRulesByRegionIdAndYear(uuid, currentYear))
        .orElseGet(() -> findRulesByCountryIdAndYear(countryId, currentYear));
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
          .set(RL_REGIME, rule.getRegime().orElse(null))
          .set(RL_PROPERTY_TYPE, rule.getPropertyType().orElse(null))
          .set(RL_CONTRACT_TYPE, rule.getContractType().orElse(null))
          .set(RL_TAX_REGIME, rule.getTaxRegime().orElse(null))
          .set(RL_TENANCY_PHASE, rule.getTenancyPhase().orElse(null))
          .set(RL_BUILD_YEAR_MIN, rule.getBuildYearMin().orElse(null))
          .set(RL_BUILD_YEAR_MAX, rule.getBuildYearMax().orElse(null))
          .set(RL_EPC_CLASS_MIN, rule.getEpcClassMin().orElse(null))
          .set(RL_EPC_CLASS_MAX, rule.getEpcClassMax().orElse(null))
          .set(RL_CONTRACT_SIGNED_AFTER, rule.getContractSignedAfter().map(Date::valueOf).orElse(null))
          .set(RL_CONTRACT_SIGNED_BEFORE, rule.getContractSignedBefore().map(Date::valueOf).orElse(null))
          .set(RL_LANDLORD_MIN_PROPERTIES, rule.getLandlordMinProperties().orElse(null))
          .set(RL_AREA_CODE, rule.getAreaCode().orElse(null))
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
          .set(RL_REGIME, rule.getRegime().orElse(null))
          .set(RL_PROPERTY_TYPE, rule.getPropertyType().orElse(null))
          .set(RL_CONTRACT_TYPE, rule.getContractType().orElse(null))
          .set(RL_TAX_REGIME, rule.getTaxRegime().orElse(null))
          .set(RL_TENANCY_PHASE, rule.getTenancyPhase().orElse(null))
          .set(RL_BUILD_YEAR_MIN, rule.getBuildYearMin().orElse(null))
          .set(RL_BUILD_YEAR_MAX, rule.getBuildYearMax().orElse(null))
          .set(RL_EPC_CLASS_MIN, rule.getEpcClassMin().orElse(null))
          .set(RL_EPC_CLASS_MAX, rule.getEpcClassMax().orElse(null))
          .set(RL_CONTRACT_SIGNED_AFTER, rule.getContractSignedAfter().map(Date::valueOf).orElse(null))
          .set(RL_CONTRACT_SIGNED_BEFORE, rule.getContractSignedBefore().map(Date::valueOf).orElse(null))
          .set(RL_LANDLORD_MIN_PROPERTIES, rule.getLandlordMinProperties().orElse(null))
          .set(RL_AREA_CODE, rule.getAreaCode().orElse(null))
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

  // ==================== Country request table ===
  private static final Table<?> REQUESTS = table("rent_regulation_country_requests");
  private static final Field<UUID> RQ_ID = field("id", UUID.class);
  private static final Field<String> RQ_IDENTIFIER = field("identifier", String.class);
  private static final Field<UUID> RQ_TEAM_ID = field("team_id", UUID.class);
  private static final Field<String> RQ_COUNTRY_NAME = field("country_name", String.class);
  private static final Field<String> RQ_NOTES = field("notes", String.class);
  private static final Field<Timestamp> RQ_CREATED_AT = field("created_at", Timestamp.class);
  private static final Field<Timestamp> RQ_UPDATED_AT = field("updated_at", Timestamp.class);
  private static final Field<String> RQ_CREATED_BY = field("created_by", String.class);
  private static final Field<String> RQ_UPDATED_BY = field("updated_by", String.class);

  // ==================== Country request operations ====================

  public boolean countryRequestExists(UUID teamId, String countryName, String createdBy) {
    return dsl.fetchExists(
        dsl.selectFrom(REQUESTS)
            .where(
                RQ_TEAM_ID
                    .eq(teamId)
                    .and(
                        org.jooq
                            .impl
                            .DSL
                            .lower(RQ_COUNTRY_NAME)
                            .eq(countryName.toLowerCase(Locale.ROOT)))
                    .and(RQ_CREATED_BY.eq(createdBy))));
  }

  public void saveCountryRequest(
      Sid identifier,
      UUID teamId,
      String countryName,
      @org.jspecify.annotations.Nullable String notes,
      String createdBy) {
    Timestamp now = Timestamp.from(clock.instant());
    dsl.insertInto(REQUESTS)
        .set(RQ_IDENTIFIER, identifier.value())
        .set(RQ_TEAM_ID, teamId)
        .set(RQ_COUNTRY_NAME, countryName)
        .set(RQ_NOTES, notes)
        .set(RQ_CREATED_AT, now)
        .set(RQ_UPDATED_AT, now)
        .set(RQ_CREATED_BY, createdBy)
        .set(RQ_UPDATED_BY, createdBy)
        .execute();
  }

  public List<CountryRegulationRequestSummary> findCountryRequestSummaries() {
    // Qualified fields to avoid ambiguity with joined table columns
    Table<?> TEAMS = table("teams");
    Table<?> USERS = table("users");
    Field<String> T_IDENTIFIER = field("teams.identifier", String.class);
    Field<String> T_NAME = field("teams.name", String.class);
    Field<String> U_IDENTIFIER = field("users.identifier", String.class);
    Field<String> U_FIRST_NAME = field("users.first_name", String.class);
    Field<String> U_LAST_NAME = field("users.last_name", String.class);
    Field<String> RQ_Q_COUNTRY_NAME =
        field("rent_regulation_country_requests.country_name", String.class);
    Field<String> RQ_Q_CREATED_BY =
        field("rent_regulation_country_requests.created_by", String.class);
    Field<String> RQ_Q_NOTES = field("rent_regulation_country_requests.notes", String.class);
    Field<Timestamp> RQ_Q_CREATED_AT =
        field("rent_regulation_country_requests.created_at", Timestamp.class);

    var rows =
        dsl.select(
                RQ_Q_COUNTRY_NAME,
                RQ_Q_CREATED_BY,
                T_IDENTIFIER,
                T_NAME,
                U_FIRST_NAME,
                U_LAST_NAME,
                RQ_Q_NOTES,
                RQ_Q_CREATED_AT)
            .from(REQUESTS)
            .join(TEAMS)
            .on(RQ_TEAM_ID.eq(field("teams.id", UUID.class)))
            .leftJoin(USERS)
            .on(RQ_Q_CREATED_BY.eq(U_IDENTIFIER))
            .orderBy(RQ_Q_COUNTRY_NAME.asc(), RQ_Q_CREATED_AT.asc())
            .fetch();

    // Group by country name (case-insensitive)
    java.util.LinkedHashMap<String, List<Record>> grouped = new java.util.LinkedHashMap<>();
    for (Record row : rows) {
      String name = row.get(RQ_Q_COUNTRY_NAME);
      grouped.computeIfAbsent(name, k -> new java.util.ArrayList<>()).add(row);
    }

    return grouped.entrySet().stream()
        .map(
            entry -> {
              String countryName = entry.getKey();
              List<Record> records = entry.getValue();

              List<CountryRegulationRequester> requesters =
                  records.stream()
                      .map(
                          r -> {
                            String firstName = Optional.ofNullable(r.get(U_FIRST_NAME)).orElse("");
                            String lastName = Optional.ofNullable(r.get(U_LAST_NAME)).orElse("");
                            String userName = (firstName + " " + lastName).trim();
                            return new CountryRegulationRequester(
                                r.get(RQ_Q_CREATED_BY),
                                userName.isEmpty() ? r.get(RQ_Q_CREATED_BY) : userName,
                                r.get(T_IDENTIFIER),
                                r.get(T_NAME),
                                Optional.ofNullable(r.get(RQ_Q_NOTES)),
                                r.get(RQ_Q_CREATED_AT).toInstant());
                          })
                      .toList();

              return new CountryRegulationRequestSummary(
                  countryName,
                  records.size(),
                  records.getFirst().get(RQ_Q_CREATED_AT).toInstant(),
                  records.getLast().get(RQ_Q_CREATED_AT).toInstant(),
                  requesters);
            })
        .sorted((a, b) -> Integer.compare(b.requestCount(), a.requestCount()))
        .toList();
  }

  public void deleteCountryRequests(String countryName) {
    dsl.deleteFrom(REQUESTS)
        .where(org.jooq.impl.DSL.lower(RQ_COUNTRY_NAME).eq(countryName.toLowerCase(Locale.ROOT)))
        .execute();
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
    rule.setRegime(Optional.ofNullable(record.get(RL_REGIME)));
    rule.setPropertyType(Optional.ofNullable(record.get(RL_PROPERTY_TYPE)));
    rule.setContractType(Optional.ofNullable(record.get(RL_CONTRACT_TYPE)));
    rule.setTaxRegime(Optional.ofNullable(record.get(RL_TAX_REGIME)));
    rule.setTenancyPhase(Optional.ofNullable(record.get(RL_TENANCY_PHASE)));
    rule.setBuildYearMin(Optional.ofNullable(record.get(RL_BUILD_YEAR_MIN)));
    rule.setBuildYearMax(Optional.ofNullable(record.get(RL_BUILD_YEAR_MAX)));
    rule.setEpcClassMin(Optional.ofNullable(record.get(RL_EPC_CLASS_MIN)));
    rule.setEpcClassMax(Optional.ofNullable(record.get(RL_EPC_CLASS_MAX)));
    Date contractSignedAfter = record.get(RL_CONTRACT_SIGNED_AFTER);
    rule.setContractSignedAfter(Optional.ofNullable(contractSignedAfter).map(Date::toLocalDate));
    Date contractSignedBefore = record.get(RL_CONTRACT_SIGNED_BEFORE);
    rule.setContractSignedBefore(Optional.ofNullable(contractSignedBefore).map(Date::toLocalDate));
    rule.setLandlordMinProperties(Optional.ofNullable(record.get(RL_LANDLORD_MIN_PROPERTIES)));
    rule.setAreaCode(Optional.ofNullable(record.get(RL_AREA_CODE)));
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
