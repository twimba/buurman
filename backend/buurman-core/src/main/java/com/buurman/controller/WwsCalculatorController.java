package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.UnitIdentifier;
import com.buurman.domain.identifier.WwsCalculationIdentifier;
import com.buurman.dto.request.WwsCalculationRequest;
import com.buurman.dto.response.WwsCalculationResponse;
import com.buurman.dto.response.WwsPreFillResponse;
import com.buurman.generated.api.WwsCalculatorApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.WwsPointsCalculatorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class WwsCalculatorController implements WwsCalculatorApi {

  private final WwsPointsCalculatorService wwsService;

  @Override
  public WwsCalculationResponse calculateWws(WwsCalculationRequest wwsCalculationRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return wwsService.calculate(wwsCalculationRequest, principal);
  }

  @Override
  public WwsCalculationResponse calculateAndSaveWws(WwsCalculationRequest wwsCalculationRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return wwsService.calculateAndSave(wwsCalculationRequest, principal);
  }

  @Override
  public WwsPreFillResponse getWwsPreFill(UnitIdentifier unitIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return wwsService.getPreFillData(unitIdentifier, principal);
  }

  @Override
  public List<WwsCalculationResponse> getWwsCalculations(PropertyIdentifier propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return wwsService.getCalculationHistory(propertyIdentifier, principal);
  }

  @Override
  public WwsCalculationResponse getLatestWwsCalculation(PropertyIdentifier propertyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return wwsService.getLatestCalculation(propertyIdentifier, principal);
  }

  @Override
  public void deleteWwsCalculation(WwsCalculationIdentifier calculationIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    wwsService.deleteCalculation(calculationIdentifier, principal);
  }

  @Override
  public List<WwsCalculationResponse> getWwsCalculationsForUnit(UnitIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return wwsService.getCalculationHistoryForUnit(identifier, principal);
  }

  @Override
  public WwsCalculationResponse getLatestWwsCalculationForUnit(UnitIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return wwsService.getLatestCalculationForUnit(identifier, principal);
  }
}
