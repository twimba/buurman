package com.buurman.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.AmenityIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.PropertyAmenityRequest;
import com.buurman.dto.response.AmenityResponse;
import com.buurman.dto.response.PropertyAmenityResponse;
import com.buurman.generated.api.AmenitiesApi;
import com.buurman.generated.api.PropertyAmenitiesApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyAmenityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AmenityController implements AmenitiesApi, PropertyAmenitiesApi {

  private final PropertyAmenityService propertyAmenityService;

  @Override
  public Map<String, List<AmenityResponse>> getAllAmenities(Optional<String> category) {
    return propertyAmenityService.getAllAmenitiesGrouped(category.orElse(null));
  }

  @Override
  public List<PropertyAmenityResponse> getPropertyAmenities(PropertyIdentifier propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyAmenityService.getPropertyAmenities(propertyIdentifier, principal);
  }

  @Override
  public PropertyAmenityResponse addAmenity(
      PropertyIdentifier propertyIdentifier, PropertyAmenityRequest propertyAmenityRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyAmenityService.addAmenity(propertyIdentifier, propertyAmenityRequest, principal);
  }

  @Override
  public void removeAmenity(
      PropertyIdentifier propertyIdentifier, AmenityIdentifier amenityIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    propertyAmenityService.removeAmenity(propertyIdentifier, amenityIdentifier, principal);
  }
}
