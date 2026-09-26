package com.buurman.mapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.buurman.domain.RentFrequency;
import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.RentRegulationRegion;
import com.buurman.domain.RentRegulationRule;
import com.buurman.domain.RentRegulationTenancyRule;
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
import com.buurman.dto.response.RentRegulationTenancyRuleResponse;
import com.buurman.util.SidGenerator;

@Component
public class RentRegulationMapper {

  private static final long STALE_THRESHOLD_DAYS = 180;

  public RentRegulationCountryResponse toCountryResponse(RentRegulationCountry country) {
    return new RentRegulationCountryResponse(
        country.getIdentifier().orElseThrow(),
        country.getCountryCode(),
        country.getCountryName(),
        country.isHasRegionalRegulations(),
        country.getSummary(),
        country.getLastReviewedAt(),
        isStale(country),
        country.getLateFeePolicy(),
        country.getLateFeeMaxPercentage(),
        country.getLateFeeNotes(),
        country.getFormalNoticeDays());
  }

  /**
   * Does not populate {@code tenancyRules} (always empty here); {@link
   * com.buurman.service.RentRegulationService#getCountryDetail} is the real assembly path for the
   * full detail response, including tenancy rules.
   */
  public RentRegulationCountryDetailResponse toCountryDetailResponse(
      RentRegulationCountry country,
      List<RentRegulationRegion> regions,
      List<RentRegulationRule> rules) {
    return new RentRegulationCountryDetailResponse(
        country.getIdentifier().orElseThrow(),
        country.getCountryCode(),
        country.getCountryName(),
        country.isHasRegionalRegulations(),
        country.getSummary(),
        country.getLastReviewedAt(),
        isStale(country),
        regions.stream().map(this::toRegionResponse).toList(),
        rules.stream().map(this::toRuleResponse).toList(),
        country.getLateFeePolicy(),
        country.getLateFeeMaxPercentage(),
        country.getLateFeeNotes(),
        country.getFormalNoticeDays(),
        List.of());
  }

  public RentRegulationRegionResponse toRegionResponse(RentRegulationRegion region) {
    return new RentRegulationRegionResponse(
        region.getIdentifier().orElseThrow(),
        region.getRegionCode(),
        region.getRegionName(),
        region.getSummary());
  }

  public RentRegulationRuleResponse toRuleResponse(RentRegulationRule rule) {
    return new RentRegulationRuleResponse(
        rule.getIdentifier().orElseThrow(),
        rule.getYear(),
        rule.getPropertyCategory(),
        rule.getMaxIncreasePercentage(),
        rule.getMaxIncreaseType(),
        rule.getIndexName(),
        rule.getIndexValue(),
        rule.getEffectiveDate(),
        rule.getNoticePeriodDays(),
        rule.getFrequency(),
        rule.getAdditionalConditions(),
        rule.getSourceUrl(),
        rule.getNotes(),
        rule.getRegime(),
        rule.getPropertyType(),
        rule.getContractType(),
        rule.getTaxRegime(),
        rule.getTenancyPhase(),
        rule.getBuildYearMin(),
        rule.getBuildYearMax(),
        rule.getEpcClassMin(),
        rule.getEpcClassMax(),
        rule.getContractSignedAfter(),
        rule.getContractSignedBefore(),
        rule.getLandlordMinProperties(),
        rule.getAreaCode());
  }

  public RentRegulationTenancyRuleResponse toTenancyRuleResponse(
      RentRegulationTenancyRule rule, Optional<String> regionCode) {
    return new RentRegulationTenancyRuleResponse(
        rule.getIdentifier().orElseThrow(),
        rule.getTopic(),
        regionCode,
        rule.getLabel(),
        rule.getValue(),
        rule.getEffectiveFrom(),
        rule.getLegalBasis(),
        rule.getSourceUrl(),
        rule.getNotes());
  }

  // ==================== Request → Domain ====================

  public RentRegulationCountry toCountry(CreateRentRegulationCountryRequest request) {
    return RentRegulationCountry.builder()
        .identifier(Optional.of(SidGenerator.newRentRegulationCountryId()))
        .countryCode(request.countryCode())
        .countryName(request.countryName())
        .hasRegionalRegulations(request.hasRegionalRegulations())
        .summary(request.summary())
        .build();
  }

  public void updateCountry(
      RentRegulationCountry existing, UpdateRentRegulationCountryRequest request) {
    existing.setCountryName(request.countryName());
    existing.setHasRegionalRegulations(request.hasRegionalRegulations());
    existing.setSummary(request.summary());
  }

  public RentRegulationRegion toRegion(CreateRentRegulationRegionRequest request, UUID countryId) {
    return RentRegulationRegion.builder()
        .identifier(Optional.of(SidGenerator.newRentRegulationRegionId()))
        .countryId(countryId)
        .regionCode(request.regionCode())
        .regionName(request.regionName())
        .summary(request.summary())
        .build();
  }

  public void updateRegion(
      RentRegulationRegion existing, UpdateRentRegulationRegionRequest request) {
    existing.setRegionName(request.regionName());
    existing.setSummary(request.summary());
  }

  public RentRegulationRule toRule(CreateRentRegulationRuleRequest request, UUID countryId) {
    return RentRegulationRule.builder()
        .identifier(Optional.of(SidGenerator.newRentRegulationRuleId()))
        .countryId(countryId)
        .year(request.year())
        .propertyCategory(request.propertyCategory())
        .maxIncreasePercentage(request.maxIncreasePercentage())
        .maxIncreaseType(request.maxIncreaseType())
        .indexName(request.indexName())
        .indexValue(request.indexValue())
        .effectiveDate(request.effectiveDate())
        .noticePeriodDays(request.noticePeriodDays())
        .frequency(request.frequency().orElse(RentFrequency.ANNUAL))
        .additionalConditions(request.additionalConditions())
        .sourceUrl(request.sourceUrl())
        .notes(request.notes())
        .regime(request.regime())
        .propertyType(request.propertyType())
        .contractType(request.contractType())
        .taxRegime(request.taxRegime())
        .tenancyPhase(request.tenancyPhase())
        .buildYearMin(request.buildYearMin())
        .buildYearMax(request.buildYearMax())
        .epcClassMin(request.epcClassMin())
        .epcClassMax(request.epcClassMax())
        .contractSignedAfter(request.contractSignedAfter())
        .contractSignedBefore(request.contractSignedBefore())
        .landlordMinProperties(request.landlordMinProperties())
        .areaCode(request.areaCode())
        .build();
  }

  public void updateRule(RentRegulationRule existing, UpdateRentRegulationRuleRequest request) {
    existing.setYear(request.year());
    existing.setPropertyCategory(request.propertyCategory());
    existing.setMaxIncreasePercentage(request.maxIncreasePercentage());
    existing.setMaxIncreaseType(request.maxIncreaseType());
    existing.setIndexName(request.indexName());
    existing.setIndexValue(request.indexValue());
    existing.setEffectiveDate(request.effectiveDate());
    existing.setNoticePeriodDays(request.noticePeriodDays());
    existing.setFrequency(request.frequency().orElse(RentFrequency.ANNUAL));
    existing.setAdditionalConditions(request.additionalConditions());
    existing.setSourceUrl(request.sourceUrl());
    existing.setNotes(request.notes());
    existing.setRegime(request.regime());
    existing.setPropertyType(request.propertyType());
    existing.setContractType(request.contractType());
    existing.setTaxRegime(request.taxRegime());
    existing.setTenancyPhase(request.tenancyPhase());
    existing.setBuildYearMin(request.buildYearMin());
    existing.setBuildYearMax(request.buildYearMax());
    existing.setEpcClassMin(request.epcClassMin());
    existing.setEpcClassMax(request.epcClassMax());
    existing.setContractSignedAfter(request.contractSignedAfter());
    existing.setContractSignedBefore(request.contractSignedBefore());
    existing.setLandlordMinProperties(request.landlordMinProperties());
    existing.setAreaCode(request.areaCode());
  }

  private boolean isStale(RentRegulationCountry country) {
    return country
        .getLastReviewedAt()
        .map(
            reviewed ->
                reviewed.plus(STALE_THRESHOLD_DAYS, ChronoUnit.DAYS).isBefore(Instant.now()))
        .orElse(true);
  }
}
