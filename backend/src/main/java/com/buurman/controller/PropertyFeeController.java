package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.PropertyFeeIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.CreatePropertyFeeRequest;
import com.buurman.dto.request.UpdatePropertyFeeRequest;
import com.buurman.dto.response.PropertyFeeResponse;
import com.buurman.generated.api.PropertyFeesApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyFeeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PropertyFeeController implements PropertyFeesApi {

  private final PropertyFeeService feeService;

  @Override
  public List<PropertyFeeResponse> listFees(PropertyIdentifier propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return feeService.listByProperty(propertyIdentifier, principal);
  }

  @Override
  public PropertyFeeResponse getFee(
      PropertyIdentifier propertyIdentifier, PropertyFeeIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return feeService.get(identifier, principal);
  }

  @Override
  public PropertyFeeResponse createFee(
      PropertyIdentifier propertyIdentifier, CreatePropertyFeeRequest createPropertyFeeRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return feeService.create(propertyIdentifier, createPropertyFeeRequest, principal);
  }

  @Override
  public PropertyFeeResponse updateFee(
      PropertyIdentifier propertyIdentifier,
      PropertyFeeIdentifier identifier,
      UpdatePropertyFeeRequest updatePropertyFeeRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return feeService.update(identifier, updatePropertyFeeRequest, principal);
  }

  @Override
  public void deleteFee(PropertyIdentifier propertyIdentifier, PropertyFeeIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    feeService.delete(identifier, principal);
  }
}
