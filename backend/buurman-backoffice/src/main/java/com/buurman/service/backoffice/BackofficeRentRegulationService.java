package com.buurman.service.backoffice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.RentRegulationRegion;
import com.buurman.domain.RentRegulationRule;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.RentRegulationRuleIdentifier;
import com.buurman.dto.request.BulkCreateRentRegulationRulesRequest;
import com.buurman.dto.request.CreateRentRegulationCountryRequest;
import com.buurman.dto.request.CreateRentRegulationRegionRequest;
import com.buurman.dto.request.CreateRentRegulationRuleRequest;
import com.buurman.dto.request.UpdateRentRegulationCountryRequest;
import com.buurman.dto.request.UpdateRentRegulationRegionRequest;
import com.buurman.dto.request.UpdateRentRegulationRuleRequest;
import com.buurman.dto.response.BulkImportResult;
import com.buurman.dto.response.RentRegulationCountryResponse;
import com.buurman.dto.response.RentRegulationRegionResponse;
import com.buurman.dto.response.RentRegulationRuleResponse;
import com.buurman.mapper.RentRegulationMapper;
import com.buurman.repository.RentRegulationRepository;
import com.buurman.security.BackofficePrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackofficeRentRegulationService {

  private final RentRegulationRepository repository;
  private final RentRegulationMapper mapper;

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public List<RentRegulationCountryResponse> listCountries() {
    return repository.findAllCountries().stream().map(mapper::toCountryResponse).toList();
  }

  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public RentRegulationCountryResponse createCountry(
      CreateRentRegulationCountryRequest request, BackofficePrincipal principal) {
    RentRegulationCountry country = mapper.toCountry(request);
    RentRegulationCountry saved = repository.saveCountry(country);

    log.info(
        "Backoffice user {} created rent regulation country {} (code={})",
        principal.getEmail().orElse("unknown"),
        saved.getIdentifier().map(Sid::value).orElse("?"),
        saved.getCountryCode());

    return mapper.toCountryResponse(saved);
  }

  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public RentRegulationCountryResponse updateCountry(
      String code, UpdateRentRegulationCountryRequest request, BackofficePrincipal principal) {
    RentRegulationCountry existing = repository.getCountryByCode(code);
    mapper.updateCountry(existing, request);
    RentRegulationCountry updated = repository.updateCountry(existing);

    log.info(
        "Backoffice user {} updated rent regulation country {}",
        principal.getEmail().orElse("unknown"),
        code);

    return mapper.toCountryResponse(updated);
  }

  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public void deleteCountry(String code, BackofficePrincipal principal) {
    RentRegulationCountry existing = repository.getCountryByCode(code);
    repository.deleteCountry(existing.getId());

    log.info(
        "Backoffice user {} deleted rent regulation country {}",
        principal.getEmail().orElse("unknown"),
        code);
  }

  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public void reviewCountry(String code, BackofficePrincipal principal) {
    RentRegulationCountry existing = repository.getCountryByCode(code);
    existing.setLastReviewedAt(java.util.Optional.of(Instant.now()));
    repository.updateCountry(existing);

    log.info(
        "Backoffice user {} marked rent regulation country {} as reviewed",
        principal.getEmail().orElse("unknown"),
        code);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public List<RentRegulationRegionResponse> listRegions(String code) {
    RentRegulationCountry country = repository.getCountryByCode(code);
    return repository.findRegionsByCountryId(country.getId()).stream()
        .map(mapper::toRegionResponse)
        .toList();
  }

  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public RentRegulationRegionResponse createRegion(
      String code, CreateRentRegulationRegionRequest request, BackofficePrincipal principal) {
    RentRegulationCountry country = repository.getCountryByCode(code);
    RentRegulationRegion region = mapper.toRegion(request, country.getId());
    RentRegulationRegion saved = repository.saveRegion(region);

    log.info(
        "Backoffice user {} created region {} for country {}",
        principal.getEmail().orElse("unknown"),
        saved.getRegionCode(),
        code);

    return mapper.toRegionResponse(saved);
  }

  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public RentRegulationRegionResponse updateRegion(
      String code,
      String regionCode,
      UpdateRentRegulationRegionRequest request,
      BackofficePrincipal principal) {
    RentRegulationCountry country = repository.getCountryByCode(code);
    RentRegulationRegion existing =
        repository.getRegionByCountryIdAndCode(country.getId(), regionCode);
    mapper.updateRegion(existing, request);
    RentRegulationRegion updated = repository.updateRegion(existing);

    log.info(
        "Backoffice user {} updated region {} for country {}",
        principal.getEmail().orElse("unknown"),
        regionCode,
        code);

    return mapper.toRegionResponse(updated);
  }

  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public void deleteRegion(String code, String regionCode, BackofficePrincipal principal) {
    RentRegulationCountry country = repository.getCountryByCode(code);
    RentRegulationRegion existing =
        repository.getRegionByCountryIdAndCode(country.getId(), regionCode);
    repository.deleteRegion(existing.getId());

    log.info(
        "Backoffice user {} deleted region {} for country {}",
        principal.getEmail().orElse("unknown"),
        regionCode,
        code);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public List<RentRegulationRuleResponse> listRules(String code) {
    RentRegulationCountry country = repository.getCountryByCode(code);
    return repository.findRulesByCountryId(country.getId()).stream()
        .map(mapper::toRuleResponse)
        .toList();
  }

  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public RentRegulationRuleResponse createRule(
      String code, CreateRentRegulationRuleRequest request, BackofficePrincipal principal) {
    RentRegulationCountry country = repository.getCountryByCode(code);
    RentRegulationRule rule = mapper.toRule(request, country.getId());
    RentRegulationRule saved = repository.saveRule(rule);

    log.info(
        "Backoffice user {} created rule for country {} (year={}, category={})",
        principal.getEmail().orElse("unknown"),
        code,
        request.year(),
        request.propertyCategory());

    return mapper.toRuleResponse(saved);
  }

  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public BulkImportResult bulkImportRules(
      String code, BulkCreateRentRegulationRulesRequest request, BackofficePrincipal principal) {
    RentRegulationCountry country = repository.getCountryByCode(code);
    List<String> errors = new ArrayList<>();
    int created = 0;

    for (int i = 0; i < request.rules().size(); i++) {
      try {
        CreateRentRegulationRuleRequest ruleRequest = request.rules().get(i);
        RentRegulationRule rule = mapper.toRule(ruleRequest, country.getId());
        repository.saveRule(rule);
        created++;
      } catch (Exception e) {
        errors.add("Rule at index " + i + ": " + e.getMessage());
      }
    }

    log.info(
        "Backoffice user {} bulk imported {} rules for country {} ({} failed)",
        principal.getEmail().orElse("unknown"),
        created,
        code,
        errors.size());

    return new BulkImportResult(request.rules().size(), created, errors.size(), errors);
  }

  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public RentRegulationRuleResponse updateRule(
      RentRegulationRuleIdentifier identifier,
      UpdateRentRegulationRuleRequest request,
      BackofficePrincipal principal) {
    RentRegulationRule existing = repository.getRuleByIdentifier(identifier);
    mapper.updateRule(existing, request);
    RentRegulationRule updated = repository.updateRule(existing);

    log.info(
        "Backoffice user {} updated rule {}",
        principal.getEmail().orElse("unknown"),
        identifier.value());

    return mapper.toRuleResponse(updated);
  }

  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public void deleteRule(RentRegulationRuleIdentifier identifier, BackofficePrincipal principal) {
    RentRegulationRule existing = repository.getRuleByIdentifier(identifier);
    repository.deleteRule(existing.getId());

    log.info(
        "Backoffice user {} deleted rule {}",
        principal.getEmail().orElse("unknown"),
        identifier.value());
  }
}
