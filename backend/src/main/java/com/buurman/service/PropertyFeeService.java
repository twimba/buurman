package com.buurman.service;

import static com.buurman.util.UlidGenerator.newPropertyFeeId;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Property;
import com.buurman.domain.PropertyFee;
import com.buurman.dto.request.CreatePropertyFeeRequest;
import com.buurman.dto.request.UpdatePropertyFeeRequest;
import com.buurman.dto.response.PropertyFeeResponse;
import com.buurman.dto.response.PropertySummary;
import com.buurman.mapper.PropertyFeeMapper;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.PropertyFeeRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyFeeService {

  private final PropertyFeeRepository feeRepository;
  private final PropertyRepository propertyRepository;
  private final PropertyFeeMapper feeMapper;
  private final PropertyMapper propertyMapper;
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyFeeResponse create(
      String propertyIdentifier, CreatePropertyFeeRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    PropertyFee fee = feeMapper.toEntity(request);
    fee.setPropertyId(property.getId());
    fee.setIdentifier(newPropertyFeeId().value());
    fee.setTeamId(teamId);
    fee.setCreatedBy(principal.getUserId());
    fee.setUpdatedBy(principal.getUserId());
    fee.setCreatedAt(clock.instant());
    fee.setUpdatedAt(clock.instant());

    if (fee.getStatus() == null) {
      fee.setStatus(PropertyFee.FeeStatus.ACTIVE);
    }

    PropertyFee saved = feeRepository.save(fee);

    log.info(
        "Created property fee {} for property {} by user {}",
        saved.getIdentifier(),
        property.getIdentifier(),
        principal.getUserId());

    return enrichResponse(saved, teamId);
  }

  @Transactional(readOnly = true)
  public PropertyFeeResponse get(String identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    PropertyFee fee = feeRepository.getByIdentifierAndTeamId(identifier, teamId);
    return enrichResponse(fee, teamId);
  }

  @Transactional(readOnly = true)
  public List<PropertyFeeResponse> listByProperty(
      String propertyIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    return feeRepository.findByPropertyIdAndTeamId(property.getId(), teamId).stream()
        .map(fee -> enrichResponse(fee, teamId))
        .toList();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyFeeResponse update(
      String identifier, UpdatePropertyFeeRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    PropertyFee fee = feeRepository.getByIdentifierAndTeamId(identifier, teamId);

    feeMapper.updateEntity(fee, request);
    fee.setUpdatedBy(principal.getUserId());
    fee.setUpdatedAt(clock.instant());

    PropertyFee updated = feeRepository.save(fee);

    log.info("Updated property fee {} by user {}", updated.getIdentifier(), principal.getUserId());

    return enrichResponse(updated, teamId);
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void delete(String identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    PropertyFee fee = feeRepository.getByIdentifierAndTeamId(identifier, teamId);

    feeRepository.softDeleteByIdAndTeamId(fee.getId(), teamId);

    log.info("Deleted property fee {} by user {}", fee.getIdentifier(), principal.getUserId());
  }

  private PropertyFeeResponse enrichResponse(PropertyFee fee, UUID teamId) {
    PropertyFeeResponse response = feeMapper.toResponse(fee);

    PropertySummary propertySummary =
        propertyRepository
            .findByIdAndTeamId(fee.getPropertyId(), teamId)
            .map(propertyMapper::toSummary)
            .orElse(null);

    return new PropertyFeeResponse(
        response.identifier(),
        Optional.ofNullable(propertySummary),
        response.feeType(),
        response.name(),
        response.annualAmount(),
        response.currency(),
        response.paymentFrequency(),
        response.dueMonths(),
        response.startDate(),
        response.endDate(),
        response.status(),
        response.notes(),
        response.createdAt(),
        response.updatedAt());
  }
}
