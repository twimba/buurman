package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.PropertyTaxIdentifier;
import com.buurman.dto.request.CreatePropertyTaxRequest;
import com.buurman.dto.request.UpdatePropertyTaxRequest;
import com.buurman.dto.response.PropertyTaxResponse;
import com.buurman.generated.api.PropertyTaxesApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyTaxService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PropertyTaxController implements PropertyTaxesApi {

  private final PropertyTaxService taxService;

  @Override
  public List<PropertyTaxResponse> listTaxes(String propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return taxService.listByProperty(PropertyIdentifier.of(propertyIdentifier), principal);
  }

  @Override
  public PropertyTaxResponse getTax(String propertyIdentifier, String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return taxService.get(PropertyTaxIdentifier.of(identifier), principal);
  }

  @Override
  public PropertyTaxResponse createTax(
      String propertyIdentifier, CreatePropertyTaxRequest createPropertyTaxRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return taxService.create(PropertyIdentifier.of(propertyIdentifier), createPropertyTaxRequest, principal);
  }

  @Override
  public PropertyTaxResponse updateTax(
      String propertyIdentifier,
      String identifier,
      UpdatePropertyTaxRequest updatePropertyTaxRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return taxService.update(PropertyTaxIdentifier.of(identifier), updatePropertyTaxRequest, principal);
  }

  @Override
  public void deleteTax(String propertyIdentifier, String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    taxService.delete(PropertyTaxIdentifier.of(identifier), principal);
  }
}
