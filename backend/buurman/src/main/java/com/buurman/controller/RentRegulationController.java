package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.RentRegulationCountryDetailResponse;
import com.buurman.dto.response.RentRegulationCountryResponse;
import com.buurman.dto.response.RentRegulationRuleResponse;
import com.buurman.generated.api.RentRegulationsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.RentRegulationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RentRegulationController implements RentRegulationsApi {

  private final RentRegulationService rentRegulationService;

  @Override
  public List<RentRegulationCountryResponse> listRentRegulationCountries() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return rentRegulationService.listCountries(principal);
  }

  @Override
  public RentRegulationCountryDetailResponse getRentRegulationCountryDetail(String code) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return rentRegulationService.getCountryDetail(code, principal);
  }

  @Override
  public List<RentRegulationRuleResponse> getCurrentRentRegulationRules(String code) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return rentRegulationService.getCurrentRules(code, principal);
  }

  @Override
  public List<RentRegulationRuleResponse> getRentRegulationRulesByYear(String code, Integer year) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return rentRegulationService.getRulesByYear(code, year, principal);
  }

  @Override
  public List<RentRegulationRuleResponse> getCurrentRegionRentRegulationRules(
      String code, String regionCode) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return rentRegulationService.getCurrentRegionRules(code, regionCode, principal);
  }
}
