package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.PropertyInsuranceIdentifier;
import com.buurman.dto.request.CreatePropertyInsuranceRequest;
import com.buurman.dto.request.UpdatePropertyInsuranceRequest;
import com.buurman.dto.response.PropertyInsuranceResponse;
import com.buurman.generated.api.PropertyInsurancesApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyInsuranceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PropertyInsuranceController implements PropertyInsurancesApi {

  private final PropertyInsuranceService insuranceService;

  @Override
  public List<PropertyInsuranceResponse> listInsurances(PropertyIdentifier propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return insuranceService.listByProperty(propertyIdentifier, principal);
  }

  @Override
  public PropertyInsuranceResponse getInsurance(
      PropertyIdentifier propertyIdentifier, PropertyInsuranceIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return insuranceService.get(identifier, principal);
  }

  @Override
  public PropertyInsuranceResponse createInsurance(
      PropertyIdentifier propertyIdentifier,
      CreatePropertyInsuranceRequest createPropertyInsuranceRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return insuranceService.create(propertyIdentifier, createPropertyInsuranceRequest, principal);
  }

  @Override
  public PropertyInsuranceResponse updateInsurance(
      PropertyIdentifier propertyIdentifier,
      PropertyInsuranceIdentifier identifier,
      UpdatePropertyInsuranceRequest updatePropertyInsuranceRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return insuranceService.update(identifier, updatePropertyInsuranceRequest, principal);
  }

  @Override
  public void deleteInsurance(
      PropertyIdentifier propertyIdentifier, PropertyInsuranceIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    insuranceService.delete(identifier, principal);
  }
}
