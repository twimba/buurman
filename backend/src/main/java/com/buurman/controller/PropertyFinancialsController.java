package com.buurman.controller;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.response.PropertyFinancialSummaryResponse;
import com.buurman.generated.api.PropertyFinancialsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyFinancialsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PropertyFinancialsController implements PropertyFinancialsApi {

  private final PropertyFinancialsService financialsService;

  @Override
  public PropertyFinancialSummaryResponse getFinancialSummary(
      PropertyIdentifier propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return financialsService.getFinancialSummary(propertyIdentifier, principal);
  }
}
