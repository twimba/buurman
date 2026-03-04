package com.buurman.service;

import static com.buurman.util.SidGenerator.newPropertyTaxId;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Property;
import com.buurman.domain.PropertyTax;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.PropertyTaxIdentifier;
import com.buurman.dto.request.CreatePropertyTaxRequest;
import com.buurman.dto.request.UpdatePropertyTaxRequest;
import com.buurman.dto.response.PropertySummary;
import com.buurman.dto.response.PropertyTaxResponse;
import com.buurman.mapper.PropertyMapper;
import com.buurman.mapper.PropertyTaxMapper;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.PropertyTaxRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyTaxService {

  private final PropertyTaxRepository taxRepository;
  private final PropertyRepository propertyRepository;
  private final PropertyTaxMapper taxMapper;
  private final PropertyMapper propertyMapper;
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyTaxResponse create(
      PropertyIdentifier propertyIdentifier,
      CreatePropertyTaxRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    PropertyTax tax = taxMapper.toEntity(request);
    tax.setPropertyId(property.getId());
    tax.setIdentifier(Optional.of(newPropertyTaxId()));
    tax.setTeamId(teamId);
    tax.setCreatedBy(principal.getUserId());
    tax.setUpdatedBy(principal.getUserId());
    tax.setCreatedAt(clock.instant());
    tax.setUpdatedAt(clock.instant());

    if (tax.getStatus() == null) {
      tax.setStatus(PropertyTax.TaxStatus.ACTIVE);
    }

    PropertyTax saved = taxRepository.save(tax);

    log.info(
        "Created property tax {} for property {} by user {}",
        saved.getIdentifier().orElseThrow(),
        property.getIdentifier().orElseThrow(),
        principal.getUserId());

    return enrichResponse(saved, teamId);
  }

  @Transactional(readOnly = true)
  public PropertyTaxResponse get(PropertyTaxIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    PropertyTax tax = taxRepository.getByIdentifierAndTeamId(identifier, teamId);
    return enrichResponse(tax, teamId);
  }

  @Transactional(readOnly = true)
  public List<PropertyTaxResponse> listByProperty(
      PropertyIdentifier propertyIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    return taxRepository.findByPropertyIdAndTeamId(property.getId(), teamId).stream()
        .map(tax -> enrichResponse(tax, teamId))
        .toList();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyTaxResponse update(
      PropertyTaxIdentifier identifier, UpdatePropertyTaxRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    PropertyTax tax = taxRepository.getByIdentifierAndTeamId(identifier, teamId);

    taxMapper.updateEntity(tax, request);
    tax.setUpdatedBy(principal.getUserId());
    tax.setUpdatedAt(clock.instant());

    PropertyTax updated = taxRepository.save(tax);

    log.info(
        "Updated property tax {} by user {}",
        updated.getIdentifier().orElseThrow(),
        principal.getUserId());

    return enrichResponse(updated, teamId);
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void delete(PropertyTaxIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    PropertyTax tax = taxRepository.getByIdentifierAndTeamId(identifier, teamId);

    taxRepository.softDeleteByIdAndTeamId(tax.getId(), teamId);

    log.info(
        "Deleted property tax {} by user {}",
        tax.getIdentifier().orElseThrow(),
        principal.getUserId());
  }

  private PropertyTaxResponse enrichResponse(PropertyTax tax, UUID teamId) {
    PropertyTaxResponse response = taxMapper.toResponse(tax);

    PropertySummary propertySummary =
        propertyRepository
            .findByIdAndTeamId(tax.getPropertyId(), teamId)
            .map(propertyMapper::toSummary)
            .orElse(null);

    return new PropertyTaxResponse(
        response.identifier(),
        Optional.ofNullable(propertySummary),
        response.taxType(),
        response.authority(),
        response.annualAmount(),
        response.currency(),
        response.paymentFrequency(),
        response.dueMonths(),
        response.taxYear(),
        response.startDate(),
        response.endDate(),
        response.status(),
        response.notes(),
        response.createdAt(),
        response.updatedAt());
  }
}
