package com.buurman.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.MaxIncreaseType;
import com.buurman.domain.RentFrequency;
import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.RentRegulationRegion;
import com.buurman.domain.RentRegulationRule;
import com.buurman.domain.Sid;
import com.buurman.dto.request.CreateRentRegulationCountryRequest;
import com.buurman.dto.request.CreateRentRegulationRegionRequest;
import com.buurman.dto.request.CreateRentRegulationRuleRequest;
import com.buurman.dto.request.UpdateRentRegulationCountryRequest;
import com.buurman.dto.request.UpdateRentRegulationRegionRequest;
import com.buurman.dto.request.UpdateRentRegulationRuleRequest;
import com.buurman.dto.response.RentRegulationCountryDetailResponse;
import com.buurman.dto.response.RentRegulationCountryResponse;
import com.buurman.dto.response.RentRegulationRegionResponse;
import com.buurman.dto.response.RentRegulationRuleResponse;

@DisplayName("RentRegulationMapper")
class RentRegulationMapperTest {

  private final RentRegulationMapper mapper = new RentRegulationMapper();

  private static final Sid COUNTRY_ID = Sid.of("RRC01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final Sid REGION_ID = Sid.of("RRR01HQJK4B2X5M3N7P8Q9R0S1T2");
  private static final Sid RULE_ID = Sid.of("RRL01HQJK4B2X5M3N7P8Q9R0S1T2");

  @Nested
  @DisplayName("toCountryResponse")
  class ToCountryResponseTest {

    @Test
    @DisplayName("maps all fields")
    void mapsAllFields() {
      RentRegulationCountry country =
          createCountry(Optional.of(Instant.now().minus(10, ChronoUnit.DAYS)));

      RentRegulationCountryResponse response = mapper.toCountryResponse(country);

      assertThat(response.identifier()).isEqualTo(COUNTRY_ID);
      assertThat(response.countryCode()).isEqualTo("NL");
      assertThat(response.countryName()).isEqualTo("Netherlands");
      assertThat(response.hasRegionalRegulations()).isTrue();
      assertThat(response.summary()).contains("Rent control summary");
      assertThat(response.lastReviewedAt()).isPresent();
      assertThat(response.stale()).isFalse();
    }

    @Test
    @DisplayName("marks as stale when never reviewed")
    void staleWhenNeverReviewed() {
      RentRegulationCountry country = createCountry(Optional.empty());

      RentRegulationCountryResponse response = mapper.toCountryResponse(country);

      assertThat(response.stale()).isTrue();
      assertThat(response.lastReviewedAt()).isEmpty();
    }

    @Test
    @DisplayName("marks as stale when reviewed over 180 days ago")
    void staleWhenReviewedLongAgo() {
      RentRegulationCountry country =
          createCountry(Optional.of(Instant.now().minus(200, ChronoUnit.DAYS)));

      RentRegulationCountryResponse response = mapper.toCountryResponse(country);

      assertThat(response.stale()).isTrue();
    }

    @Test
    @DisplayName("not stale when recently reviewed")
    void notStaleWhenRecent() {
      RentRegulationCountry country =
          createCountry(Optional.of(Instant.now().minus(10, ChronoUnit.DAYS)));

      RentRegulationCountryResponse response = mapper.toCountryResponse(country);

      assertThat(response.stale()).isFalse();
    }
  }

  @Nested
  @DisplayName("toCountryDetailResponse")
  class ToCountryDetailResponseTest {

    @Test
    @DisplayName("maps country fields, regions, and rules")
    void mapsAllParts() {
      RentRegulationCountry country =
          createCountry(Optional.of(Instant.now().minus(10, ChronoUnit.DAYS)));
      List<RentRegulationRegion> regions = List.of(createRegion());
      List<RentRegulationRule> rules = List.of(createRule());

      RentRegulationCountryDetailResponse response =
          mapper.toCountryDetailResponse(country, regions, rules);

      assertThat(response.identifier()).isEqualTo(COUNTRY_ID);
      assertThat(response.countryCode()).isEqualTo("NL");
      assertThat(response.regions()).hasSize(1);
      assertThat(response.regions().get(0).regionCode()).isEqualTo("NH");
      assertThat(response.rules()).hasSize(1);
      assertThat(response.rules().get(0).year()).isEqualTo(2026);
    }

    @Test
    @DisplayName("maps empty regions and rules lists")
    void mapsEmptyLists() {
      RentRegulationCountry country = createCountry(Optional.empty());

      RentRegulationCountryDetailResponse response =
          mapper.toCountryDetailResponse(country, List.of(), List.of());

      assertThat(response.regions()).isEmpty();
      assertThat(response.rules()).isEmpty();
    }
  }

  @Nested
  @DisplayName("toRegionResponse")
  class ToRegionResponseTest {

    @Test
    @DisplayName("maps all fields")
    void mapsAllFields() {
      RentRegulationRegion region = createRegion();

      RentRegulationRegionResponse response = mapper.toRegionResponse(region);

      assertThat(response.identifier()).isEqualTo(REGION_ID);
      assertThat(response.regionCode()).isEqualTo("NH");
      assertThat(response.regionName()).isEqualTo("North Holland");
      assertThat(response.summary()).contains("Regional rules");
    }

    @Test
    @DisplayName("maps empty summary")
    void mapsEmptySummary() {
      RentRegulationRegion region =
          RentRegulationRegion.builder()
              .identifier(Optional.of(REGION_ID))
              .countryId(UUID.randomUUID())
              .regionCode("ZH")
              .regionName("South Holland")
              .createdAt(Instant.now())
              .updatedAt(Instant.now())
              .build();

      RentRegulationRegionResponse response = mapper.toRegionResponse(region);

      assertThat(response.summary()).isEmpty();
    }
  }

  @Nested
  @DisplayName("toRuleResponse")
  class ToRuleResponseTest {

    @Test
    @DisplayName("maps all fields")
    void mapsAllFields() {
      RentRegulationRule rule = createRule();

      RentRegulationRuleResponse response = mapper.toRuleResponse(rule);

      assertThat(response.identifier()).isEqualTo(RULE_ID);
      assertThat(response.year()).isEqualTo(2026);
      assertThat(response.propertyCategory()).isEqualTo("RESIDENTIAL");
      assertThat(response.sector()).contains("REGULATED");
      assertThat(response.maxIncreasePercentage()).contains(new BigDecimal("4.10"));
      assertThat(response.maxIncreaseType()).isEqualTo(MaxIncreaseType.CPI_LINKED);
      assertThat(response.indexName()).contains("CPI");
      assertThat(response.frequency()).isEqualTo(RentFrequency.ANNUAL);
      assertThat(response.notes()).contains("Based on CPI");
    }

    @Test
    @DisplayName("maps optional fields as empty when not set")
    void mapsOptionalFieldsAsEmpty() {
      RentRegulationRule rule =
          RentRegulationRule.builder()
              .identifier(Optional.of(RULE_ID))
              .countryId(UUID.randomUUID())
              .year(2026)
              .propertyCategory("COMMERCIAL")
              .maxIncreaseType(MaxIncreaseType.NEGOTIATED)
              .frequency(RentFrequency.ANNUAL)
              .createdAt(Instant.now())
              .updatedAt(Instant.now())
              .build();

      RentRegulationRuleResponse response = mapper.toRuleResponse(rule);

      assertThat(response.sector()).isEmpty();
      assertThat(response.maxIncreasePercentage()).isEmpty();
      assertThat(response.indexName()).isEmpty();
      assertThat(response.indexValue()).isEmpty();
      assertThat(response.effectiveDate()).isEmpty();
      assertThat(response.noticePeriodDays()).isEmpty();
      assertThat(response.additionalConditions()).isEmpty();
      assertThat(response.sourceUrl()).isEmpty();
      assertThat(response.notes()).isEmpty();
    }
  }

  @Nested
  @DisplayName("toCountry")
  class ToCountryTest {

    @Test
    @DisplayName("creates country from request with generated identifier")
    void createsCountryFromRequest() {
      CreateRentRegulationCountryRequest request =
          new CreateRentRegulationCountryRequest("DE", "Germany", false, Optional.of("Summary"));

      RentRegulationCountry country = mapper.toCountry(request);

      assertThat(country.getIdentifier()).isPresent();
      assertThat(country.getCountryCode()).isEqualTo("DE");
      assertThat(country.getCountryName()).isEqualTo("Germany");
      assertThat(country.isHasRegionalRegulations()).isFalse();
      assertThat(country.getSummary()).contains("Summary");
    }
  }

  @Nested
  @DisplayName("updateCountry")
  class UpdateCountryTest {

    @Test
    @DisplayName("updates mutable fields")
    void updatesFields() {
      RentRegulationCountry existing = createCountry(Optional.empty());
      UpdateRentRegulationCountryRequest request =
          new UpdateRentRegulationCountryRequest("Updated Name", false, Optional.of("New summary"));

      mapper.updateCountry(existing, request);

      assertThat(existing.getCountryName()).isEqualTo("Updated Name");
      assertThat(existing.isHasRegionalRegulations()).isFalse();
      assertThat(existing.getSummary()).contains("New summary");
    }
  }

  @Nested
  @DisplayName("toRegion")
  class ToRegionTest {

    @Test
    @DisplayName("creates region with country ID and generated identifier")
    void createsRegion() {
      UUID countryId = UUID.randomUUID();
      CreateRentRegulationRegionRequest request =
          new CreateRentRegulationRegionRequest("BY", "Bavaria", Optional.of("Bavaria rules"));

      RentRegulationRegion region = mapper.toRegion(request, countryId);

      assertThat(region.getIdentifier()).isPresent();
      assertThat(region.getCountryId()).isEqualTo(countryId);
      assertThat(region.getRegionCode()).isEqualTo("BY");
      assertThat(region.getRegionName()).isEqualTo("Bavaria");
      assertThat(region.getSummary()).contains("Bavaria rules");
    }
  }

  @Nested
  @DisplayName("updateRegion")
  class UpdateRegionTest {

    @Test
    @DisplayName("updates mutable fields")
    void updatesFields() {
      RentRegulationRegion existing = createRegion();
      UpdateRentRegulationRegionRequest request =
          new UpdateRentRegulationRegionRequest("Updated Region", Optional.of("Updated summary"));

      mapper.updateRegion(existing, request);

      assertThat(existing.getRegionName()).isEqualTo("Updated Region");
      assertThat(existing.getSummary()).contains("Updated summary");
    }
  }

  @Nested
  @DisplayName("toRule")
  class ToRuleTest {

    @Test
    @DisplayName("creates rule from request")
    void createsRule() {
      UUID countryId = UUID.randomUUID();
      CreateRentRegulationRuleRequest request =
          new CreateRentRegulationRuleRequest(
              2026,
              "RESIDENTIAL",
              Optional.of("FREE"),
              Optional.of(new BigDecimal("5.50")),
              MaxIncreaseType.FIXED_PERCENTAGE,
              Optional.empty(),
              Optional.empty(),
              Optional.of(LocalDate.of(2026, 7, 1)),
              Optional.of(30),
              Optional.of(RentFrequency.MONTHLY),
              Optional.empty(),
              Optional.empty(),
              Optional.of("Test note"));

      RentRegulationRule rule = mapper.toRule(request, countryId);

      assertThat(rule.getIdentifier()).isPresent();
      assertThat(rule.getCountryId()).isEqualTo(countryId);
      assertThat(rule.getYear()).isEqualTo(2026);
      assertThat(rule.getFrequency()).isEqualTo(RentFrequency.MONTHLY);
    }

    @Test
    @DisplayName("defaults frequency to ANNUAL when empty")
    void defaultsFrequencyToAnnual() {
      UUID countryId = UUID.randomUUID();
      CreateRentRegulationRuleRequest request =
          new CreateRentRegulationRuleRequest(
              2026,
              "RESIDENTIAL",
              Optional.empty(),
              Optional.empty(),
              MaxIncreaseType.FROZEN,
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty());

      RentRegulationRule rule = mapper.toRule(request, countryId);

      assertThat(rule.getFrequency()).isEqualTo(RentFrequency.ANNUAL);
    }
  }

  @Nested
  @DisplayName("updateRule")
  class UpdateRuleTest {

    @Test
    @DisplayName("updates all fields")
    void updatesAllFields() {
      RentRegulationRule existing = createRule();
      UpdateRentRegulationRuleRequest request =
          new UpdateRentRegulationRuleRequest(
              2027,
              "COMMERCIAL",
              Optional.of("FREE"),
              Optional.of(new BigDecimal("6.00")),
              MaxIncreaseType.FIXED_PERCENTAGE,
              Optional.of("New Index"),
              Optional.of(new BigDecimal("3.00")),
              Optional.of(LocalDate.of(2027, 1, 1)),
              Optional.of(60),
              Optional.of(RentFrequency.QUARTERLY),
              Optional.of("New conditions"),
              Optional.of("https://example.com"),
              Optional.of("Updated note"));

      mapper.updateRule(existing, request);

      assertThat(existing.getYear()).isEqualTo(2027);
      assertThat(existing.getPropertyCategory()).isEqualTo("COMMERCIAL");
      assertThat(existing.getFrequency()).isEqualTo(RentFrequency.QUARTERLY);
    }

    @Test
    @DisplayName("defaults frequency to ANNUAL when empty")
    void defaultsFrequencyToAnnual() {
      RentRegulationRule existing = createRule();
      UpdateRentRegulationRuleRequest request =
          new UpdateRentRegulationRuleRequest(
              2027,
              "RESIDENTIAL",
              Optional.empty(),
              Optional.empty(),
              MaxIncreaseType.FROZEN,
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty());

      mapper.updateRule(existing, request);

      assertThat(existing.getFrequency()).isEqualTo(RentFrequency.ANNUAL);
    }
  }

  // ---- helpers ----

  private RentRegulationCountry createCountry(Optional<Instant> lastReviewedAt) {
    return RentRegulationCountry.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(COUNTRY_ID))
        .countryCode("NL")
        .countryName("Netherlands")
        .hasRegionalRegulations(true)
        .summary(Optional.of("Rent control summary"))
        .lastReviewedAt(lastReviewedAt)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  private RentRegulationRegion createRegion() {
    return RentRegulationRegion.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(REGION_ID))
        .countryId(UUID.randomUUID())
        .regionCode("NH")
        .regionName("North Holland")
        .summary(Optional.of("Regional rules"))
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  private RentRegulationRule createRule() {
    return RentRegulationRule.builder()
        .id(UUID.randomUUID())
        .identifier(Optional.of(RULE_ID))
        .countryId(UUID.randomUUID())
        .year(2026)
        .propertyCategory("RESIDENTIAL")
        .sector(Optional.of("REGULATED"))
        .maxIncreasePercentage(Optional.of(new BigDecimal("4.10")))
        .maxIncreaseType(MaxIncreaseType.CPI_LINKED)
        .indexName(Optional.of("CPI"))
        .indexValue(Optional.of(new BigDecimal("2.50")))
        .effectiveDate(Optional.of(LocalDate.of(2026, 7, 1)))
        .noticePeriodDays(Optional.of(30))
        .frequency(RentFrequency.ANNUAL)
        .additionalConditions(Optional.of("None"))
        .sourceUrl(Optional.of("https://gov.nl"))
        .notes(Optional.of("Based on CPI"))
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }
}
