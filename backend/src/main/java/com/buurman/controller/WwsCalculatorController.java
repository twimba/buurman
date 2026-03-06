package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.WwsCalculationRequest;
import com.buurman.dto.response.WwsCalculationResponse;
import com.buurman.dto.response.WwsPreFillResponse;
import com.buurman.generated.api.WwsCalculatorApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.WwsPointsCalculatorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class WwsCalculatorController implements WwsCalculatorApi {

  private final WwsPointsCalculatorService wwsService;

  @Override
  public WwsCalculationResponse calculateWws(
      @Valid WwsCalculationRequest wwsCalculationRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return wwsService.calculate(wwsCalculationRequest, principal);
  }

  @Override
  public WwsCalculationResponse calculateAndSaveWws(
      @Valid WwsCalculationRequest wwsCalculationRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return wwsService.calculateAndSave(wwsCalculationRequest, principal);
  }

  @Override
  public WwsPreFillResponse getWwsPreFill(PropertyIdentifier propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return wwsService.getPreFillData(propertyIdentifier, principal);
  }

  @Override
  public List<WwsCalculationResponse> getWwsCalculations(
      PropertyIdentifier propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return wwsService.getCalculationHistory(propertyIdentifier, principal);
  }

  @Override
  public WwsCalculationResponse getLatestWwsCalculation(
      PropertyIdentifier propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return wwsService.getLatestCalculation(propertyIdentifier, principal);
  }
}
