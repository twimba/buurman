package com.buurman.service;

import com.buurman.domain.Property;
import com.buurman.domain.PropertyOutdoorArea;
import com.buurman.dto.request.PropertyOutdoorAreaRequest;
import com.buurman.dto.response.PropertyOutdoorAreaResponse;
import com.buurman.repository.PropertyOutdoorAreaRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.EntityPrefix;
import com.buurman.util.UlidGenerator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PropertyOutdoorAreaService {

    private final PropertyOutdoorAreaRepository outdoorAreaRepository;
    private final PropertyRepository propertyRepository;

    public PropertyOutdoorAreaService(
            PropertyOutdoorAreaRepository outdoorAreaRepository,
            PropertyRepository propertyRepository) {
        this.outdoorAreaRepository = outdoorAreaRepository;
        this.propertyRepository = propertyRepository;
    }

    public List<PropertyOutdoorAreaResponse> getOutdoorAreas(String propertyIdentifier, UserPrincipal principal) {
        Property property = resolveProperty(propertyIdentifier, principal);
        return outdoorAreaRepository.findByPropertyIdAndTeamId(property.getId(), principal.getTeamId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PropertyOutdoorAreaResponse createOutdoorArea(
            String propertyIdentifier,
            PropertyOutdoorAreaRequest request,
            UserPrincipal principal) {

        Property property = resolveProperty(propertyIdentifier, principal);

        PropertyOutdoorArea area = new PropertyOutdoorArea();
        area.setIdentifier(UlidGenerator.generate(EntityPrefix.POA));
        area.setPropertyId(property.getId());
        area.setTeamId(principal.getTeamId());
        area.setType(request.type());
        area.setAreaValue(request.areaValue());
        area.setAreaUnit(request.areaUnit() != null ? request.areaUnit() : "sqm");
        area.setCreatedBy(principal.getUserId());
        area.setUpdatedBy(principal.getUserId());

        PropertyOutdoorArea saved = outdoorAreaRepository.save(area);
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PropertyOutdoorAreaResponse updateOutdoorArea(
            String propertyIdentifier,
            String areaIdentifier,
            PropertyOutdoorAreaRequest request,
            UserPrincipal principal) {

        resolveProperty(propertyIdentifier, principal);

        PropertyOutdoorArea area = outdoorAreaRepository.findByIdentifierAndTeamId(areaIdentifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Outdoor area not found"));

        area.setType(request.type());
        area.setAreaValue(request.areaValue());
        if (request.areaUnit() != null) {
            area.setAreaUnit(request.areaUnit());
        }
        area.setUpdatedBy(principal.getUserId());

        PropertyOutdoorArea updated = outdoorAreaRepository.save(area);
        return toResponse(updated);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public void deleteOutdoorArea(
            String propertyIdentifier,
            String areaIdentifier,
            UserPrincipal principal) {

        resolveProperty(propertyIdentifier, principal);

        PropertyOutdoorArea area = outdoorAreaRepository.findByIdentifierAndTeamId(areaIdentifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Outdoor area not found"));

        outdoorAreaRepository.softDeleteByIdAndTeamId(area.getId(), principal.getTeamId());
    }

    private Property resolveProperty(String propertyIdentifier, UserPrincipal principal) {
        return propertyRepository.findByIdentifierAndTeamId(propertyIdentifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
    }

    private PropertyOutdoorAreaResponse toResponse(PropertyOutdoorArea area) {
        return new PropertyOutdoorAreaResponse(
                area.getIdentifier(),
                area.getType(),
                area.getAreaValue(),
                area.getAreaUnit(),
                area.getCreatedAt(),
                area.getUpdatedAt()
        );
    }
}
