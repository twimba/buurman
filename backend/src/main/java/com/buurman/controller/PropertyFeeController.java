package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.Ulid;
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
  public List<PropertyFeeResponse> listFees(String propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return feeService.listByProperty(Ulid.of(propertyIdentifier), principal);
  }

  @Override
  public PropertyFeeResponse getFee(String propertyIdentifier, String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return feeService.get(Ulid.of(identifier), principal);
  }

  @Override
  public PropertyFeeResponse createFee(
      String propertyIdentifier, CreatePropertyFeeRequest createPropertyFeeRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return feeService.create(Ulid.of(propertyIdentifier), createPropertyFeeRequest, principal);
  }

  @Override
  public PropertyFeeResponse updateFee(
      String propertyIdentifier,
      String identifier,
      UpdatePropertyFeeRequest updatePropertyFeeRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return feeService.update(Ulid.of(identifier), updatePropertyFeeRequest, principal);
  }

  @Override
  public void deleteFee(String propertyIdentifier, String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    feeService.delete(Ulid.of(identifier), principal);
  }
}
