package com.buurman.service;

import static com.buurman.util.UlidGenerator.newFinancingId;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Property;
import com.buurman.domain.PropertyFinancing;
import com.buurman.domain.Ulid;
import com.buurman.domain.identifier.PropertyFinancingIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.CreatePropertyFinancingRequest;
import com.buurman.dto.request.UpdatePropertyFinancingRequest;
import com.buurman.dto.response.PropertyFinancingResponse;
import com.buurman.mapper.PropertyFinancingMapper;
import com.buurman.repository.PropertyFinancingRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyFinancingService {

  private final PropertyFinancingRepository financingRepository;
  private final PropertyRepository propertyRepository;
  private final PropertyFinancingMapper financingMapper;
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyFinancingResponse create(
      PropertyIdentifier propertyIdentifier, CreatePropertyFinancingRequest request, UserPrincipal principal) {

    Property property =
        propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, principal.requireTeamId());

    PropertyFinancing financing = financingMapper.toEntity(request);
    financing.setPropertyId(property.getId());
    financing.setIdentifier(Optional.of(newFinancingId()));
    financing.setTeamId(principal.requireTeamId());
    financing.setCreatedBy(principal.getUserId());
    financing.setUpdatedBy(principal.getUserId());
    financing.setCreatedAt(clock.instant());
    financing.setUpdatedAt(clock.instant());

    PropertyFinancing saved = financingRepository.save(financing);

    log.info(
        "Created property financing {} for property {} by user {}",
        saved.getIdentifier().orElseThrow(),
        property.getIdentifier().orElseThrow(),
        principal.getUserId());

    return toResponse(saved, property.getIdentifier().orElseThrow());
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyFinancingResponse update(
      PropertyFinancingIdentifier financingIdentifier, UpdatePropertyFinancingRequest request, UserPrincipal principal) {

    PropertyFinancing financing =
        financingRepository.getByIdentifierAndTeamId(
            financingIdentifier, principal.requireTeamId());

    financingMapper.updateEntity(financing, request);
    financing.setUpdatedBy(principal.getUserId());
    financing.setUpdatedAt(clock.instant());

    PropertyFinancing updated = financingRepository.save(financing);

    log.info(
        "Updated property financing {} by user {}", updated.getIdentifier().orElseThrow(), principal.getUserId());

    Ulid propertyIdentifier =
        resolvePropertyIdentifier(updated.getPropertyId(), principal.requireTeamId());

    return toResponse(updated, propertyIdentifier);
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void delete(PropertyFinancingIdentifier financingIdentifier, UserPrincipal principal) {
    PropertyFinancing financing =
        financingRepository.getByIdentifierAndTeamId(
            financingIdentifier, principal.requireTeamId());

    financingRepository.softDeleteByIdAndTeamId(financing.getId(), principal.requireTeamId());

    log.info(
        "Deleted property financing {} by user {}",
        financing.getIdentifier().orElseThrow(),
        principal.getUserId());
  }

  @Transactional(readOnly = true)
  public List<PropertyFinancingResponse> listByProperty(
      PropertyIdentifier propertyIdentifier, UserPrincipal principal) {

    Property property =
        propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, principal.requireTeamId());

    return financingRepository
        .findByPropertyIdAndTeamId(property.getId(), principal.requireTeamId())
        .stream()
        .map(f -> toResponse(f, property.getIdentifier().orElseThrow()))
        .toList();
  }

  @Transactional(readOnly = true)
  public PropertyFinancingResponse getFinancing(
      PropertyFinancingIdentifier financingIdentifier, UserPrincipal principal) {

    PropertyFinancing financing =
        financingRepository.getByIdentifierAndTeamId(
            financingIdentifier, principal.requireTeamId());

    Ulid propertyIdentifier =
        resolvePropertyIdentifier(financing.getPropertyId(), principal.requireTeamId());

    return toResponse(financing, propertyIdentifier);
  }

  private PropertyFinancingResponse toResponse(
      PropertyFinancing financing, Ulid propertyIdentifier) {

    PropertyFinancingResponse mapped = financingMapper.toResponse(financing);
    return new PropertyFinancingResponse(
        mapped.identifier(),
        propertyIdentifier,
        mapped.financingType(),
        mapped.rateType(),
        mapped.lenderName(),
        mapped.loanNumber(),
        mapped.originalAmount(),
        mapped.originalAmountCurrency(),
        mapped.currentBalance(),
        mapped.currentBalanceCurrency(),
        mapped.interestRate(),
        mapped.monthlyPayment(),
        mapped.monthlyPaymentCurrency(),
        mapped.paymentVariable(),
        mapped.startDate(),
        mapped.endDate(),
        mapped.termMonths(),
        mapped.status(),
        mapped.notes(),
        mapped.createdAt(),
        mapped.updatedAt());
  }

  private Ulid resolvePropertyIdentifier(java.util.UUID propertyId, java.util.UUID teamId) {
    return propertyRepository
        .findByIdAndTeamId(propertyId, teamId)
        .flatMap(Property::getIdentifier)
        .orElseThrow();
  }
}
