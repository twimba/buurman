package com.buurman.controller.backoffice;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.backoffice.CostProviderId;
import com.buurman.dto.request.backoffice.cost.SetManualCostRequest;
import com.buurman.dto.response.backoffice.cost.CostOverviewResponse;
import com.buurman.exception.NotFoundException;
import com.buurman.generated.backoffice.api.BackofficeCostApi;
import com.buurman.security.SecurityUtils;
import com.buurman.service.backoffice.cost.CostService;

import lombok.RequiredArgsConstructor;

/** Cost overview, on-demand refresh, and manual-amount editing for the dedicated Costs page. */
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

  @Override
  public CostOverviewResponse setManualCost(String provider, SetManualCostRequest request) {
    CostProviderId id = parseProvider(provider);
    String updatedBy = SecurityUtils.getBackofficePrincipal().getEmail().orElse("unknown");
    return costService.setManualAmount(id, Math.round(request.amountEur() * 100), updatedBy);
  }

  private static CostProviderId parseProvider(String provider) {
    try {
      return CostProviderId.valueOf(provider.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new NotFoundException("Unknown cost provider: " + provider);
    }
  }
}
