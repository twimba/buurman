package com.buurman.service;

import static com.buurman.util.UlidGenerator.newValuationId;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Property;
import com.buurman.domain.PropertyValuation;
import com.buurman.dto.request.CreatePropertyValuationRequest;
import com.buurman.dto.request.UpdatePropertyValuationRequest;
import com.buurman.dto.response.PropertyValuationResponse;
import com.buurman.mapper.PropertyValuationMapper;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.PropertyValuationRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyValuationService {

  private final PropertyValuationRepository valuationRepository;
  private final PropertyRepository propertyRepository;
  private final PropertyValuationMapper valuationMapper;
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyValuationResponse create(
      String propertyIdentifier, CreatePropertyValuationRequest request, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, principal.requireTeamId());

    PropertyValuation valuation = valuationMapper.toEntity(request);
    valuation.setIdentifier(newValuationId().value());
    valuation.setPropertyId(property.getId());
    valuation.setTeamId(principal.requireTeamId());
    valuation.setCreatedBy(principal.getUserId());
    valuation.setUpdatedBy(principal.getUserId());
    valuation.setCreatedAt(clock.instant());
    valuation.setUpdatedAt(clock.instant());

    PropertyValuation saved = valuationRepository.save(valuation);

    log.info(
        "Created property valuation {} for property {} by user {}",
        saved.getIdentifier(),
        propertyIdentifier,
        principal.getUserId());

    return valuationMapper.toResponse(saved);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyValuationResponse update(
      String identifier, UpdatePropertyValuationRequest request, UserPrincipal principal) {
    PropertyValuation valuation =
        valuationRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    valuationMapper.updateEntity(valuation, request);
    valuation.setUpdatedBy(principal.getUserId());
    valuation.setUpdatedAt(clock.instant());

    PropertyValuation saved = valuationRepository.save(valuation);

    log.info(
        "Updated property valuation {} by user {}", saved.getIdentifier(), principal.getUserId());

    return valuationMapper.toResponse(saved);
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void delete(String identifier, UserPrincipal principal) {
    PropertyValuation valuation =
        valuationRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    valuationRepository.softDeleteByIdAndTeamId(valuation.getId(), principal.requireTeamId());

    log.info(
        "Deleted property valuation {} by user {}",
        valuation.getIdentifier(),
        principal.getUserId());
  }

  @Transactional(readOnly = true)
  public List<PropertyValuationResponse> listByProperty(
      String propertyIdentifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, principal.requireTeamId());

    return valuationRepository
        .findByPropertyIdAndTeamId(property.getId(), principal.requireTeamId())
        .stream()
        .map(valuationMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public Optional<PropertyValuationResponse> getLatestByProperty(
      String propertyIdentifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, principal.requireTeamId());

    return valuationRepository
        .findLatestByPropertyIdAndTeamId(property.getId(), principal.requireTeamId())
        .map(valuationMapper::toResponse);
  }
}
