package com.buurman.service;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contract;
import com.buurman.domain.Property;
import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.RentRegulationRegion;
import com.buurman.domain.RentRegulationRule;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.ApplyRentIncreasesRequest;
import com.buurman.dto.request.CreateRentPeriodRequest;
import com.buurman.dto.request.RentIncreaseItem;
import com.buurman.dto.request.RentIncreasePreviewRequest;
import com.buurman.dto.response.ApplyRentIncreasesResponse;
import com.buurman.dto.response.RentIncreaseContractPreview;
import com.buurman.dto.response.RentIncreaseCountrySummary;
import com.buurman.dto.response.RentIncreasePreviewResponse;
import com.buurman.dto.response.RentIncreaseResult;
import com.buurman.dto.response.RentIncreaseSummary;
import com.buurman.dto.response.RentRegulationRuleResponse;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.RentRegulationRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RentIncreaseService {

  private final ContractRepository contractRepository;
  private final PropertyRepository propertyRepository;
  private final RentRegulationRepository rentRegulationRepository;
  private final ContractRentPeriodService contractRentPeriodService;
  private final Clock clock;

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public RentIncreasePreviewResponse preview(
      RentIncreasePreviewRequest request, UserPrincipal principal) {
    int year = request.year();
    UUID teamId = principal.requireTeamId();
    List<Contract> activeContracts = contractRepository.findActiveByTeamId(teamId);

    // Group contracts by country code (from the contract's property)
    Map<String, List<ContractWithProperty>> contractsByCountry = new LinkedHashMap<>();
    for (Contract contract : activeContracts) {
      Property property =
          propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId).orElse(null);
      if (property == null) {
        continue;
      }
      String countryCode = contract.getCountryCode().orElse(property.getCountryCode());
      contractsByCountry
          .computeIfAbsent(countryCode, k -> new ArrayList<>())
          .add(new ContractWithProperty(contract, property));
    }

    // Build country summaries and contract previews
    List<RentIncreaseCountrySummary> countrySummaries = new ArrayList<>();
    List<RentIncreaseContractPreview> contractPreviews = new ArrayList<>();

    for (Map.Entry<String, List<ContractWithProperty>> entry : contractsByCountry.entrySet()) {
      String countryCode = entry.getKey();
      List<ContractWithProperty> contracts = entry.getValue();

      Optional<RentRegulationCountry> regulationCountry =
          rentRegulationRepository.findCountryByCode(countryCode);

      // Fetch national-level rules (region_id IS NULL) for the country summary
      List<RentRegulationRule> nationalRules =
          regulationCountry
              .map(
                  c ->
                      rentRegulationRepository.findNationalRulesByCountryIdAndYear(c.getId(), year))
              .orElse(List.of());

      List<RentRegulationRuleResponse> ruleResponses =
          nationalRules.stream().map(this::toRuleResponse).toList();

      countrySummaries.add(
          new RentIncreaseCountrySummary(
              countryCode,
              regulationCountry.map(RentRegulationCountry::getCountryName).orElse(countryCode),
              contracts.size(),
              !nationalRules.isEmpty(),
              ruleResponses));

      // Build per-contract previews with region-aware rule matching
      for (ContractWithProperty cwp : contracts) {
        Contract contract = cwp.contract();
        Property property = cwp.property();

        // Resolve region-specific rules if property has a region
        List<RentRegulationRule> applicableRules =
            resolveApplicableRules(regulationCountry, property.getRegionCode(), year);

        // Find min/max percentage from applicable rules
        Optional<BigDecimal> minPercent =
            applicableRules.stream()
                .map(RentRegulationRule::getMaxIncreasePercentage)
                .flatMap(Optional::stream)
                .min(BigDecimal::compareTo);
        Optional<BigDecimal> maxPercent =
            applicableRules.stream()
                .map(RentRegulationRule::getMaxIncreasePercentage)
                .flatMap(Optional::stream)
                .max(BigDecimal::compareTo);

        // Suggested effective date from the first rule with one
        Optional<java.time.LocalDate> suggestedDate =
            applicableRules.stream()
                .map(RentRegulationRule::getEffectiveDate)
                .flatMap(Optional::stream)
                .findFirst();

        String propertyAddress =
            property.getStreet() + ", " + property.getPostalCode() + " " + property.getCity();

        contractPreviews.add(
            new RentIncreaseContractPreview(
                ContractIdentifier.of(contract.getIdentifier().orElseThrow().value()),
                com.buurman.domain.identifier.PropertyIdentifier.of(
                    property.getIdentifier().orElseThrow().value()),
                property.getStreet(),
                propertyAddress,
                property.getCountryCode(),
                property.getRegionCode(),
                contract.getRentAmount().value(),
                contract.getRentAmount().currency(),
                minPercent,
                maxPercent,
                suggestedDate));
      }
    }

    return new RentIncreasePreviewResponse(year, countrySummaries, contractPreviews);
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  @Transactional
  public ApplyRentIncreasesResponse apply(
      ApplyRentIncreasesRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    List<RentIncreaseResult> results = new ArrayList<>();
    int totalUpdated = 0;
    int totalRentPeriods = 0;
    int totalPaymentsCancelled = 0;
    int totalPaymentsGenerated = 0;
    int totalFailed = 0;

    for (RentIncreaseItem item : request.increases()) {
      try {
        Contract contract =
            contractRepository.getByIdentifierAndTeamId(item.contractIdentifier(), teamId);

        if (contract.getStatus() != Contract.ContractStatus.ACTIVE) {
          results.add(errorResult(item, contract, "Contract is not active"));
          totalFailed++;
          continue;
        }

        BigDecimal currentRent = contract.getRentAmount().value();
        BigDecimal multiplier =
            BigDecimal.ONE.add(
                item.increasePercentage().divide(BigDecimal.valueOf(100), 10, HALF_UP));
        BigDecimal newRent = currentRent.multiply(multiplier).setScale(2, HALF_UP);

        // Delegate to ContractRentPeriodService.addRentPeriod
        CreateRentPeriodRequest rentPeriodRequest =
            new CreateRentPeriodRequest(newRent, item.effectiveDate(), Optional.empty());
        contractRentPeriodService.addRentPeriod(
            item.contractIdentifier(), rentPeriodRequest, principal);

        Property property =
            propertyRepository.findByIdAndTeamId(contract.getPropertyId(), teamId).orElse(null);
        String propertyName = property != null ? property.getStreet() : "Unknown";

        results.add(
            new RentIncreaseResult(
                item.contractIdentifier(),
                propertyName,
                true,
                currentRent,
                newRent,
                item.effectiveDate(),
                0,
                0,
                Optional.empty()));

        totalUpdated++;
        totalRentPeriods++;
      } catch (Exception e) {
        log.error(
            "Failed to apply rent increase for contract {}: {}",
            item.contractIdentifier(),
            e.getMessage(),
            e);

        results.add(
            new RentIncreaseResult(
                item.contractIdentifier(),
                "Unknown",
                false,
                ZERO,
                ZERO,
                item.effectiveDate(),
                0,
                0,
                Optional.of(e.getMessage() != null ? e.getMessage() : "Unknown error")));
        totalFailed++;
      }
    }

    RentIncreaseSummary summary =
        new RentIncreaseSummary(
            totalUpdated,
            totalRentPeriods,
            totalPaymentsCancelled,
            totalPaymentsGenerated,
            totalFailed);

    return new ApplyRentIncreasesResponse(results, summary);
  }

  // --- Private helpers ---

  private List<RentRegulationRule> resolveApplicableRules(
      Optional<RentRegulationCountry> regulationCountry, Optional<String> regionCode, int year) {
    if (regulationCountry.isEmpty()) {
      return List.of();
    }
    RentRegulationCountry country = regulationCountry.get();

    // If property has a region, fetch region-specific rules
    if (regionCode.isPresent()) {
      Optional<RentRegulationRegion> region =
          rentRegulationRepository.findRegionByCode(country.getId(), regionCode.get());
      if (region.isPresent()) {
        return rentRegulationRepository.findRulesByRegionIdAndYear(region.get().getId(), year);
      }
    }

    // Fall back to national-level rules (region_id IS NULL)
    return rentRegulationRepository.findNationalRulesByCountryIdAndYear(country.getId(), year);
  }

  private RentIncreaseResult errorResult(RentIncreaseItem item, Contract contract, String error) {
    return new RentIncreaseResult(
        item.contractIdentifier(),
        "Unknown",
        false,
        contract.getRentAmount().value(),
        contract.getRentAmount().value(),
        item.effectiveDate(),
        0,
        0,
        Optional.of(error));
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

  private record ContractWithProperty(Contract contract, Property property) {}
}
