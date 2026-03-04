package com.buurman.service;

import static com.buurman.util.SidGenerator.newAcquisitionId;

import java.time.Clock;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Property;
import com.buurman.domain.PropertyAcquisition;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.UpsertPropertyAcquisitionRequest;
import com.buurman.dto.response.PropertyAcquisitionResponse;
import com.buurman.mapper.PropertyAcquisitionMapper;
import com.buurman.repository.PropertyAcquisitionRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PropertyAcquisitionService {

  private final PropertyAcquisitionRepository acquisitionRepository;
  private final PropertyRepository propertyRepository;
  private final PropertyAcquisitionMapper acquisitionMapper;
  private final Clock clock;

  @Transactional(readOnly = true)
  public Optional<PropertyAcquisitionResponse> getByProperty(
      PropertyIdentifier propertyIdentifier, UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, principal.requireTeamId());

    return acquisitionRepository
        .findByPropertyIdAndTeamId(property.getId(), principal.requireTeamId())
        .map(acquisitionMapper::toResponse);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyAcquisitionResponse upsert(
      PropertyIdentifier propertyIdentifier,
      UpsertPropertyAcquisitionRequest request,
      UserPrincipal principal) {
    Property property =
        propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, principal.requireTeamId());

    Optional<PropertyAcquisition> existing =
        acquisitionRepository.findByPropertyIdAndTeamId(
            property.getId(), principal.requireTeamId());

    PropertyAcquisition acquisition;
    if (existing.isPresent()) {
      // Update
      acquisition = existing.get();
      acquisitionMapper.updateEntity(acquisition, request);
      acquisition.setUpdatedBy(principal.getUserId());
      acquisition.setUpdatedAt(clock.instant());

      log.info(
          "Updated property acquisition {} for property {} by user {}",
          acquisition.getIdentifier().orElseThrow(),
          propertyIdentifier,
          principal.getUserId());
    } else {
      // Create
      acquisition = acquisitionMapper.toEntity(request);
      acquisition.setIdentifier(Optional.of(newAcquisitionId()));
      acquisition.setPropertyId(property.getId());
      acquisition.setTeamId(principal.requireTeamId());
      acquisition.setCreatedBy(principal.getUserId());
      acquisition.setUpdatedBy(principal.getUserId());
      acquisition.setCreatedAt(clock.instant());
      acquisition.setUpdatedAt(clock.instant());

      log.info(
          "Created property acquisition {} for property {} by user {}",
          acquisition.getIdentifier().orElseThrow(),
          propertyIdentifier,
          principal.getUserId());
    }

    PropertyAcquisition saved = acquisitionRepository.save(acquisition);
    return acquisitionMapper.toResponse(saved);
  }
}
