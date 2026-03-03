package com.buurman.service;

import static com.buurman.util.UlidGenerator.newPropertyOutdoorAreaId;

import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Property;
import com.buurman.domain.PropertyOutdoorArea;
import com.buurman.domain.Ulid;
import com.buurman.dto.request.PropertyOutdoorAreaRequest;
import com.buurman.dto.response.PropertyOutdoorAreaResponse;
import com.buurman.repository.PropertyOutdoorAreaRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PropertyOutdoorAreaService {

  private final PropertyOutdoorAreaRepository outdoorAreaRepository;
  private final PropertyRepository propertyRepository;

  public List<PropertyOutdoorAreaResponse> getOutdoorAreas(
      Ulid propertyIdentifier, UserPrincipal principal) {
    Property property = resolveProperty(propertyIdentifier, principal);
    return outdoorAreaRepository
        .findByPropertyIdAndTeamId(property.getId(), principal.requireTeamId())
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyOutdoorAreaResponse createOutdoorArea(
      Ulid propertyIdentifier, PropertyOutdoorAreaRequest request, UserPrincipal principal) {

    Property property = resolveProperty(propertyIdentifier, principal);

    PropertyOutdoorArea area = new PropertyOutdoorArea();
    area.setIdentifier(Optional.of(newPropertyOutdoorAreaId()));
    area.setPropertyId(property.getId());
    area.setTeamId(principal.requireTeamId());
    area.setType(request.type());
    area.setAreaValue(request.areaValue());
    area.setAreaUnit(request.areaUnit().orElse("sqm"));
    area.setCreatedBy(principal.getUserId());
    area.setUpdatedBy(principal.getUserId());

    PropertyOutdoorArea saved = outdoorAreaRepository.save(area);
    return toResponse(saved);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PropertyOutdoorAreaResponse updateOutdoorArea(
      Ulid propertyIdentifier,
      Ulid areaIdentifier,
      PropertyOutdoorAreaRequest request,
      UserPrincipal principal) {

    resolveProperty(propertyIdentifier, principal);

    PropertyOutdoorArea area =
        outdoorAreaRepository.getByIdentifierAndTeamId(areaIdentifier, principal.requireTeamId());

    area.setType(request.type());
    area.setAreaValue(request.areaValue());
    request.areaUnit().ifPresent(area::setAreaUnit);
    area.setUpdatedBy(principal.getUserId());

    PropertyOutdoorArea updated = outdoorAreaRepository.save(area);
    return toResponse(updated);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public void deleteOutdoorArea(
      Ulid propertyIdentifier, Ulid areaIdentifier, UserPrincipal principal) {

    resolveProperty(propertyIdentifier, principal);

    PropertyOutdoorArea area =
        outdoorAreaRepository.getByIdentifierAndTeamId(areaIdentifier, principal.requireTeamId());

    outdoorAreaRepository.softDeleteByIdAndTeamId(area.getId(), principal.requireTeamId());
  }

  private Property resolveProperty(Ulid propertyIdentifier, UserPrincipal principal) {
    return propertyRepository.getByIdentifierAndTeamId(
        propertyIdentifier, principal.requireTeamId());
  }

  private PropertyOutdoorAreaResponse toResponse(PropertyOutdoorArea area) {
    return new PropertyOutdoorAreaResponse(
        area.getIdentifier().orElseThrow(),
        area.getType(),
        area.getAreaValue(),
        Optional.ofNullable(area.getAreaUnit()),
        area.getCreatedAt(),
        Optional.ofNullable(area.getUpdatedAt()));
  }
}
