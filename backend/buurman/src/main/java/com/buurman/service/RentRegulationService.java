package com.buurman.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.RentRegulationRegion;
import com.buurman.domain.RentRegulationRule;
import com.buurman.dto.response.RentRegulationCountryDetailResponse;
import com.buurman.dto.response.RentRegulationCountryResponse;
import com.buurman.dto.response.RentRegulationRegionResponse;
import com.buurman.dto.response.RentRegulationRuleResponse;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.RentRegulationRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RentRegulationService {

  private static final Duration STALE_THRESHOLD = Duration.ofDays(180);

  private final RentRegulationRepository rentRegulationRepository;
  private final Clock clock;

  @PreAuthorize("isAuthenticated()")
  public List<RentRegulationCountryResponse> listCountries(UserPrincipal principal) {
    return rentRegulationRepository.findAllCountries().stream()
        .map(this::toCountryResponse)
        .toList();
  }

  @PreAuthorize("isAuthenticated()")
  public RentRegulationCountryDetailResponse getCountryDetail(
      String countryCode, UserPrincipal principal) {
    RentRegulationCountry country =
        rentRegulationRepository
            .findCountryByCode(countryCode)
            .orElseThrow(() -> new NotFoundException("Country not found: " + countryCode));

    List<RentRegulationRegionResponse> regions =
        rentRegulationRepository.findRegionsByCountryId(country.getId()).stream()
            .map(this::toRegionResponse)
            .toList();

    List<RentRegulationRuleResponse> rules =
        rentRegulationRepository.findRulesByCountryId(country.getId()).stream()
            .map(this::toRuleResponse)
            .toList();

    return new RentRegulationCountryDetailResponse(
        country.getIdentifier().orElseThrow(),
        country.getCountryCode(),
        country.getCountryName(),
        country.isHasRegionalRegulations(),
        country.getSummary(),
        country.getLastReviewedAt(),
        isStale(country),
        regions,
        rules);
  }

  @PreAuthorize("isAuthenticated()")
  public List<RentRegulationRuleResponse> getCurrentRules(
      String countryCode, UserPrincipal principal) {
    RentRegulationCountry country =
        rentRegulationRepository
            .findCountryByCode(countryCode)
            .orElseThrow(() -> new NotFoundException("Country not found: " + countryCode));

    return rentRegulationRepository.findCurrentRules(country.getId(), Optional.empty()).stream()
        .map(this::toRuleResponse)
        .toList();
  }

  @PreAuthorize("isAuthenticated()")
  public List<RentRegulationRuleResponse> getCurrentRegionRules(
      String countryCode, String regionCode, UserPrincipal principal) {
    RentRegulationCountry country =
        rentRegulationRepository
            .findCountryByCode(countryCode)
            .orElseThrow(() -> new NotFoundException("Country not found: " + countryCode));

    RentRegulationRegion region =
        rentRegulationRepository
            .findRegionByCode(country.getId(), regionCode)
            .orElseThrow(() -> new NotFoundException("Region not found: " + regionCode));

    return rentRegulationRepository
        .findRulesByRegionIdAndYear(region.getId(), java.time.LocalDate.now(clock).getYear())
        .stream()
        .map(this::toRuleResponse)
        .toList();
  }

  @PreAuthorize("isAuthenticated()")
  public List<RentRegulationRuleResponse> getRulesByYear(
      String countryCode, int year, UserPrincipal principal) {
    RentRegulationCountry country =
        rentRegulationRepository
            .findCountryByCode(countryCode)
            .orElseThrow(() -> new NotFoundException("Country not found: " + countryCode));

    return rentRegulationRepository.findRulesByCountryIdAndYear(country.getId(), year).stream()
        .map(this::toRuleResponse)
        .toList();
  }

  // --- Private helpers ---

  private boolean isStale(RentRegulationCountry country) {
    return country
        .getLastReviewedAt()
        .map(reviewed -> reviewed.plus(STALE_THRESHOLD).isBefore(Instant.now(clock)))
        .orElse(true);
  }

  private RentRegulationCountryResponse toCountryResponse(RentRegulationCountry country) {
    return new RentRegulationCountryResponse(
        country.getIdentifier().orElseThrow(),
        country.getCountryCode(),
        country.getCountryName(),
        country.isHasRegionalRegulations(),
        country.getSummary(),
        country.getLastReviewedAt(),
        isStale(country));
  }

  private RentRegulationRegionResponse toRegionResponse(RentRegulationRegion region) {
    return new RentRegulationRegionResponse(
        region.getIdentifier().orElseThrow(),
        region.getRegionCode(),
        region.getRegionName(),
        region.getSummary());
  }

  private RentRegulationRuleResponse toRuleResponse(RentRegulationRule rule) {
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
}
