package com.buurman.controller;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.CreatePropertyValuationRequest;
import com.buurman.dto.request.UpdatePropertyValuationRequest;
import com.buurman.dto.response.PropertyValuationResponse;
import com.buurman.generated.api.PropertyValuationsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyValuationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PropertyValuationController implements PropertyValuationsApi {

  private final PropertyValuationService valuationService;

  @Override
  public List<PropertyValuationResponse> listValuations(String propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return valuationService.listByProperty(propertyIdentifier, principal);
  }

  @Override
  public @Nullable PropertyValuationResponse getLatestValuation(String propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return valuationService.getLatestByProperty(propertyIdentifier, principal).orElse(null);
  }

  @Override
  public PropertyValuationResponse createValuation(
      String propertyIdentifier,
      CreatePropertyValuationRequest createPropertyValuationRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return valuationService.create(
        propertyIdentifier, createPropertyValuationRequest, principal);
  }

  @Override
  public PropertyValuationResponse updateValuation(
      String propertyIdentifier,
      String identifier,
      UpdatePropertyValuationRequest updatePropertyValuationRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return valuationService.update(identifier, updatePropertyValuationRequest, principal);
  }

  @Override
  public void deleteValuation(String propertyIdentifier, String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    valuationService.delete(identifier, principal);
  }
}
