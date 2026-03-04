package com.buurman.controller;

import java.util.List;
import java.util.Map;

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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AmenityController implements AmenitiesApi, PropertyAmenitiesApi {

  private final PropertyAmenityService propertyAmenityService;

  @Override
  public Map<String, List<AmenityResponse>> getAllAmenities(String category) {
    return propertyAmenityService.getAllAmenitiesGrouped(category);
  }

  @Override
  public List<PropertyAmenityResponse> getPropertyAmenities(String propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyAmenityService.getPropertyAmenities(PropertyIdentifier.of(propertyIdentifier), principal);
  }

  @Override
  public PropertyAmenityResponse addAmenity(
      String propertyIdentifier, @Valid PropertyAmenityRequest propertyAmenityRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return propertyAmenityService.addAmenity(PropertyIdentifier.of(propertyIdentifier), propertyAmenityRequest, principal);
  }

  @Override
  public void removeAmenity(String propertyIdentifier, String amenityIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    propertyAmenityService.removeAmenity(PropertyIdentifier.of(propertyIdentifier), AmenityIdentifier.of(amenityIdentifier), principal);
  }
}
