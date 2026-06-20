package com.buurman.controller.backoffice;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.backoffice.cost.CostOverviewResponse;
import com.buurman.generated.backoffice.api.BackofficeCostApi;
import com.buurman.service.backoffice.cost.CostService;

import lombok.RequiredArgsConstructor;

/** Cost overview + on-demand snapshot refresh for the dedicated Costs page. */
@RestController
@RequiredArgsConstructor
public class BackofficeCostController implements BackofficeCostApi {

  private final CostService costService;

  @Override
  public CostOverviewResponse getCostOverview() {
    return costService.overview();
  }

  @Override
  public CostOverviewResponse refreshCost() {
    costService.snapshotNow();
    return costService.overview();
  }
}
