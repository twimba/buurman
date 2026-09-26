package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.UnitIdentifier;
import com.buurman.dto.request.BulkCreateUnitsRequest;
import com.buurman.dto.request.CreateUnitRequest;
import com.buurman.dto.request.UpdateUnitAmenitiesRequest;
import com.buurman.dto.request.UpdateUnitRequest;
import com.buurman.dto.request.UpdateUnitResidentialDetailsRequest;
import com.buurman.dto.response.AmenityResponse;
import com.buurman.dto.response.UnitGridRowResponse;
import com.buurman.dto.response.UnitResidentialDetailsResponse;
import com.buurman.dto.response.UnitResponse;
import com.buurman.generated.api.UnitsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.UnitAmenityService;
import com.buurman.service.UnitResidentialDetailsService;
import com.buurman.service.UnitService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UnitController implements UnitsApi {

  private final UnitService unitService;
  private final UnitResidentialDetailsService unitResidentialDetailsService;
  private final UnitAmenityService unitAmenityService;

  @Override
  public List<UnitGridRowResponse> listUnits(PropertyIdentifier propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return unitService.listUnits(propertyIdentifier, principal);
  }

  @Override
  public UnitResponse createUnit(
      PropertyIdentifier propertyIdentifier, CreateUnitRequest createUnitRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return unitService.createUnit(propertyIdentifier, createUnitRequest, principal);
  }

  @Override
  public List<UnitResponse> bulkCreateUnits(
      PropertyIdentifier propertyIdentifier, BulkCreateUnitsRequest bulkCreateUnitsRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return unitService.bulkCreateUnits(propertyIdentifier, bulkCreateUnitsRequest, principal);
  }

  @Override
  public UnitResponse getUnit(UnitIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return unitService.getUnit(identifier, principal);
  }

  @Override
  public UnitResponse updateUnit(UnitIdentifier identifier, UpdateUnitRequest updateUnitRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return unitService.updateUnit(identifier, updateUnitRequest, principal);
  }

  @Override
  public void deleteUnit(UnitIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    unitService.deleteUnit(identifier, principal);
  }

  @Override
  public UnitResidentialDetailsResponse getUnitResidentialDetails(UnitIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return unitResidentialDetailsService.getResidentialDetails(identifier, principal);
  }

  @Override
  public UnitResidentialDetailsResponse updateUnitResidentialDetails(
      UnitIdentifier identifier,
      UpdateUnitResidentialDetailsRequest updateUnitResidentialDetailsRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return unitResidentialDetailsService.updateResidentialDetails(
        identifier, updateUnitResidentialDetailsRequest, principal);
  }

  @Override
  public List<AmenityResponse> getUnitAmenities(UnitIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return unitAmenityService.getAmenities(identifier, principal);
  }

  @Override
  public List<AmenityResponse> updateUnitAmenities(
      UnitIdentifier identifier, UpdateUnitAmenitiesRequest updateUnitAmenitiesRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return unitAmenityService.replaceAmenities(identifier, updateUnitAmenitiesRequest, principal);
  }
}
