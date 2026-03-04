package com.buurman.service;

import static com.buurman.util.UlidGenerator.newInsuranceId;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Property;
import com.buurman.domain.PropertyInsurance;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.PropertyInsuranceIdentifier;
import com.buurman.dto.request.CreatePropertyInsuranceRequest;
import com.buurman.dto.request.UpdatePropertyInsuranceRequest;
import com.buurman.dto.response.PropertyInsuranceResponse;
import com.buurman.dto.response.PropertySummary;
import com.buurman.mapper.PropertyInsuranceMapper;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.PropertyInsuranceRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyInsuranceService {

  private final PropertyInsuranceRepository insuranceRepository;
  private final PropertyRepository propertyRepository;
  private final PropertyInsuranceMapper insuranceMapper;
  private final PropertyMapper propertyMapper;
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyInsuranceResponse create(
      PropertyIdentifier propertyIdentifier, CreatePropertyInsuranceRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    PropertyInsurance insurance = insuranceMapper.toEntity(request);
    insurance.setPropertyId(property.getId());
    insurance.setIdentifier(Optional.of(newInsuranceId()));
    insurance.setTeamId(teamId);
    insurance.setCreatedBy(principal.getUserId());
    insurance.setUpdatedBy(principal.getUserId());
    insurance.setCreatedAt(clock.instant());
    insurance.setUpdatedAt(clock.instant());

    if (insurance.getStatus() == null) {
      insurance.setStatus(PropertyInsurance.InsuranceStatus.ACTIVE);
    }

    PropertyInsurance saved = insuranceRepository.save(insurance);

    log.info(
        "Created property insurance {} for property {} by user {}",
        saved.getIdentifier().orElseThrow(),
        property.getIdentifier().orElseThrow(),
        principal.getUserId());

    return enrichResponse(saved, teamId);
  }

  @Transactional(readOnly = true)
  public PropertyInsuranceResponse get(PropertyInsuranceIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    PropertyInsurance insurance = insuranceRepository.getByIdentifierAndTeamId(identifier, teamId);
    return enrichResponse(insurance, teamId);
  }

  @Transactional(readOnly = true)
  public List<PropertyInsuranceResponse> listByProperty(
      PropertyIdentifier propertyIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    return insuranceRepository.findByPropertyIdAndTeamId(property.getId(), teamId).stream()
        .map(insurance -> enrichResponse(insurance, teamId))
        .toList();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyInsuranceResponse update(
      PropertyInsuranceIdentifier identifier, UpdatePropertyInsuranceRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    PropertyInsurance insurance = insuranceRepository.getByIdentifierAndTeamId(identifier, teamId);

    insuranceMapper.updateEntity(insurance, request);
    insurance.setUpdatedBy(principal.getUserId());
    insurance.setUpdatedAt(clock.instant());

    PropertyInsurance updated = insuranceRepository.save(insurance);

    log.info(
        "Updated property insurance {} by user {}", updated.getIdentifier().orElseThrow(), principal.getUserId());

    return enrichResponse(updated, teamId);
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void delete(PropertyInsuranceIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    PropertyInsurance insurance = insuranceRepository.getByIdentifierAndTeamId(identifier, teamId);

    insuranceRepository.softDeleteByIdAndTeamId(insurance.getId(), teamId);

    log.info(
        "Deleted property insurance {} by user {}",
        insurance.getIdentifier().orElseThrow(),
        principal.getUserId());
  }

  private PropertyInsuranceResponse enrichResponse(PropertyInsurance insurance, UUID teamId) {
    PropertyInsuranceResponse response = insuranceMapper.toResponse(insurance);

    PropertySummary propertySummary =
        propertyRepository
            .findByIdAndTeamId(insurance.getPropertyId(), teamId)
            .map(propertyMapper::toSummary)
            .orElse(null);

    return new PropertyInsuranceResponse(
        response.identifier(),
        Optional.ofNullable(propertySummary),
        response.insuranceType(),
        response.provider(),
        response.policyNumber(),
        response.coverageAmount(),
        response.coverageAmountCurrency(),
        response.annualPremium(),
        response.annualPremiumCurrency(),
        response.paymentFrequency(),
        response.startDate(),
        response.endDate(),
        response.status(),
        response.notes(),
        response.createdAt(),
        response.updatedAt());
  }
}
