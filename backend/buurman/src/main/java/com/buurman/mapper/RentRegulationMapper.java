package com.buurman.mapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Component;

import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.RentRegulationRegion;
import com.buurman.domain.RentRegulationRule;
import com.buurman.dto.response.RentRegulationCountryDetailResponse;
import com.buurman.dto.response.RentRegulationCountryResponse;
import com.buurman.dto.response.RentRegulationRegionResponse;
import com.buurman.dto.response.RentRegulationRuleResponse;

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
        isStale(country));
  }

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
        rules.stream().map(this::toRuleResponse).toList());
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
        rule.getSector(),
        rule.getMaxIncreasePercentage(),
        rule.getMaxIncreaseType(),
        rule.getIndexName(),
        rule.getIndexValue(),
        rule.getEffectiveDate(),
        rule.getNoticePeriodDays(),
        rule.getFrequency(),
        rule.getAdditionalConditions(),
        rule.getSourceUrl(),
        rule.getNotes());
  }

  private boolean isStale(RentRegulationCountry country) {
    return country
        .getLastReviewedAt()
        .map(reviewed -> reviewed.plus(STALE_THRESHOLD_DAYS, ChronoUnit.DAYS).isBefore(Instant.now()))
        .orElse(true);
  }
}
