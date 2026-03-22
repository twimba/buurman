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
  public List<RentPeriodResponse> getRentTimeline(ContractIdentifier contractIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return rentPeriodService.getRentTimeline(contractIdentifier, principal);
  }

  @Override
  public RentPeriodResponse addRentPeriod(
      ContractIdentifier contractIdentifier, CreateRentPeriodRequest createRentPeriodRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return rentPeriodService
        .addRentPeriod(contractIdentifier, createRentPeriodRequest, principal)
        .rentPeriodResponse();
  }

  @Override
  public RentPeriodResponse updateRentPeriod(
      ContractIdentifier contractIdentifier,
      ContractRentPeriodIdentifier periodIdentifier,
      UpdateRentPeriodRequest updateRentPeriodRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return rentPeriodService.updateRentPeriod(
        contractIdentifier, periodIdentifier, updateRentPeriodRequest, principal);
  }

  @Override
  public void deleteRentPeriod(
      ContractIdentifier contractIdentifier, ContractRentPeriodIdentifier periodIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    rentPeriodService.deleteRentPeriod(contractIdentifier, periodIdentifier, principal);
  }
}
