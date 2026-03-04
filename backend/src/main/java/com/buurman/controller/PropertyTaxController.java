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
  public List<PropertyTaxResponse> listTaxes(PropertyIdentifier propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return taxService.listByProperty(propertyIdentifier, principal);
  }

  @Override
  public PropertyTaxResponse getTax(
      PropertyIdentifier propertyIdentifier, PropertyTaxIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return taxService.get(identifier, principal);
  }

  @Override
  public PropertyTaxResponse createTax(
      PropertyIdentifier propertyIdentifier, CreatePropertyTaxRequest createPropertyTaxRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return taxService.create(propertyIdentifier, createPropertyTaxRequest, principal);
  }

  @Override
  public PropertyTaxResponse updateTax(
      PropertyIdentifier propertyIdentifier,
      PropertyTaxIdentifier identifier,
      UpdatePropertyTaxRequest updatePropertyTaxRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return taxService.update(identifier, updatePropertyTaxRequest, principal);
  }

  @Override
  public void deleteTax(PropertyIdentifier propertyIdentifier, PropertyTaxIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    taxService.delete(identifier, principal);
  }
}
