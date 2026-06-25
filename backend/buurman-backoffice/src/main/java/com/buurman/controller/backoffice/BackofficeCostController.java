package com.buurman.controller.backoffice;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.backoffice.CostProviderId;
import com.buurman.dto.request.backoffice.cost.AddFxPairRequest;
import com.buurman.dto.request.backoffice.cost.BackfillFxRequest;
import com.buurman.dto.request.backoffice.cost.SetFxRateRequest;
import com.buurman.dto.request.backoffice.cost.SetManualCostRequest;
import com.buurman.dto.response.backoffice.cost.CostOverviewResponse;
import com.buurman.dto.response.backoffice.cost.FxPairsResponse;
import com.buurman.dto.response.backoffice.cost.FxRatesResponse;
import com.buurman.exception.NotFoundException;
import com.buurman.generated.backoffice.api.BackofficeCostApi;
import com.buurman.security.SecurityUtils;
import com.buurman.service.backoffice.cost.CostService;
import com.buurman.service.backoffice.cost.FxRateService;

import lombok.RequiredArgsConstructor;

/** Cost overview, on-demand refresh, and manual-amount + FX-rate editing for the Costs page. */
@RestController
@RequiredArgsConstructor
public class BackofficeCostController implements BackofficeCostApi {

  private final CostService costService;
  private final FxRateService fxRateService;

  @Override
  public CostOverviewResponse getCostOverview() {
    return costService.overview();
  }

  @Override
  public CostOverviewResponse refreshCost() {
    costService.refreshNow();
    return costService.overview();
  }

  @Override
  public CostOverviewResponse setManualCost(String provider, SetManualCostRequest request) {
    CostProviderId id = parseProvider(provider);
    String updatedBy = SecurityUtils.getBackofficePrincipal().getEmail().orElse("unknown");
    return costService.setManualAmount(id, Math.round(request.amountEur() * 100), updatedBy);
  }

  @Override
  public FxRatesResponse getFxRates() {
    return fxRateService.currentRates();
  }

  @Override
  public FxRatesResponse setFxRate(String currency, SetFxRateRequest request) {
    return fxRateService.setManualRate(currency, request.rate(), request.date());
  }

  @Override
  public FxRatesResponse getFxRateHistory(Optional<String> currency, Optional<Integer> limit) {
    return fxRateService.history(currency.orElse(null), limit.orElse(null));
  }

  @Override
  public FxRatesResponse refreshFxRates() {
    return fxRateService.refreshNow();
  }

  @Override
  public FxRatesResponse deleteFxRate(String currency, LocalDate date) {
    return fxRateService.deleteRate(currency, date);
  }

  @Override
  public FxRatesResponse backfillFxRates(BackfillFxRequest request) {
    return fxRateService.backfill(request.since());
  }

  @Override
  public FxPairsResponse getFxPairs() {
    return fxRateService.listPairs();
  }

  @Override
  public FxPairsResponse addFxPair(AddFxPairRequest request) {
    String by = SecurityUtils.getBackofficePrincipal().getEmail().orElse("unknown");
    return fxRateService.addPair(request.currency(), by);
  }

  @Override
  public FxPairsResponse removeFxPair(String currency) {
    return fxRateService.removePair(currency);
  }

  private static CostProviderId parseProvider(String provider) {
    try {
      return CostProviderId.valueOf(provider.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new NotFoundException("Unknown cost provider: " + provider);
    }
  }
}
