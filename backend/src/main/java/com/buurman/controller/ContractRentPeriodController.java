package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.ContractRentPeriodIdentifier;
import com.buurman.dto.request.CreateRentPeriodRequest;
import com.buurman.dto.request.UpdateRentPeriodRequest;
import com.buurman.dto.response.RentPeriodResponse;
import com.buurman.generated.api.ContractRentPeriodsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ContractRentPeriodService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ContractRentPeriodController implements ContractRentPeriodsApi {

  private final ContractRentPeriodService rentPeriodService;

  @Override
  public List<RentPeriodResponse> getRentTimeline(String contractIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return rentPeriodService.getRentTimeline(ContractIdentifier.of(contractIdentifier), principal);
  }

  @Override
  public RentPeriodResponse addRentPeriod(
      String contractIdentifier, CreateRentPeriodRequest createRentPeriodRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return rentPeriodService.addRentPeriod(ContractIdentifier.of(contractIdentifier), createRentPeriodRequest, principal);
  }

  @Override
  public RentPeriodResponse updateRentPeriod(
      String contractIdentifier,
      String periodIdentifier,
      UpdateRentPeriodRequest updateRentPeriodRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return rentPeriodService.updateRentPeriod(
        ContractIdentifier.of(contractIdentifier), ContractRentPeriodIdentifier.of(periodIdentifier), updateRentPeriodRequest, principal);
  }

  @Override
  public void deleteRentPeriod(String contractIdentifier, String periodIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    rentPeriodService.deleteRentPeriod(ContractIdentifier.of(contractIdentifier), ContractRentPeriodIdentifier.of(periodIdentifier), principal);
  }
}
