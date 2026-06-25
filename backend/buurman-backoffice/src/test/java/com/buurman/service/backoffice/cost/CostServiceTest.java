package com.buurman.service.backoffice.cost;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.backoffice.CostProviderId;
import com.buurman.domain.backoffice.CostSourceType;
import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.dto.response.backoffice.cost.CostOverviewResponse;
import com.buurman.dto.response.backoffice.cost.CostWatchResponse;
import com.buurman.dto.response.backoffice.cost.ProviderCost;
import com.buurman.repository.backoffice.CostManualAmountRepository;
import com.buurman.repository.backoffice.CostSnapshotRepository;
import com.buurman.repository.backoffice.CostSnapshotRepository.LatestProviderCost;
import com.buurman.repository.backoffice.DashboardAggregateRepository;
import com.buurman.repository.backoffice.FxRateRepository;
import com.buurman.service.backoffice.cost.CostSource.ProviderReading;
import com.fasterxml.jackson.databind.ObjectMapper;

@DisplayName("CostService")
class CostServiceTest {

  private final CostSnapshotRepository snapshotRepository = mock(CostSnapshotRepository.class);
  private final DashboardAggregateRepository aggregateRepository =
      mock(DashboardAggregateRepository.class);
  private final ManualCostResolver manualResolver = mock(ManualCostResolver.class);
  private final CostManualAmountRepository manualAmountRepository =
      mock(CostManualAmountRepository.class);
  private final FxRateRepository fxRateRepository = mock(FxRateRepository.class);
  private final FxConverter fx =
      new FxConverter(
          new CostProperties("EUR", Map.of("USD", 0.9), null, null, null, Map.of(), null),
          fxRateRepository);
  private final Clock clock = Clock.systemUTC();

  private CostService serviceWith(CostSource... sources) {
    CostService service =
        new CostService(
            clock,
            List.of(sources),
            manualResolver,
            manualAmountRepository,
            fx,
            snapshotRepository,
            aggregateRepository,
            new ObjectMapper(),
            selfProviderHolder);
    selfProviderHolder.set(service);
    return service;
  }

  // Stand-in for the Spring self-proxy: returns the same instance, so persistSnapshots runs
  // directly (no proxy/transaction in unit tests).
  private final SelfHolder selfProviderHolder = new SelfHolder();

  private static final class SelfHolder
      implements org.springframework.beans.factory.ObjectProvider<CostService> {
    private CostService instance;

    void set(CostService instance) {
      this.instance = instance;
    }

    @Override
    public CostService getObject() {
      return instance;
    }

    @Override
    public CostService getObject(Object... args) {
      return instance;
    }

    @Override
    public CostService getIfAvailable() {
      return instance;
    }

    @Override
    public CostService getIfUnique() {
      return instance;
    }
  }

  private static CostSource source(ProviderReading reading) {
    return new CostSource() {
      @Override
      public CostProviderId id() {
        return reading.provider();
      }

      @Override
      public ProviderReading read() {
        return reading;
      }
    };
  }

  @Test
  @DisplayName("snapshotNow converts to EUR and skips unavailable sources")
  void snapshotConvertsAndSkips() {
    CostService service =
        serviceWith(
            source(
                ProviderReading.of(
                    CostProviderId.TWILIO, CostSourceType.ACTUAL, "USD", 1000, List.of())),
            source(
                ProviderReading.of(
                    CostProviderId.HETZNER, CostSourceType.ESTIMATED, "EUR", 2000, List.of())),
            source(ProviderReading.unavailable(CostProviderId.CLOUDFLARE, "not set")));
    when(manualResolver.amountEurMinor(any())).thenReturn(Optional.empty());

    service.snapshotNow();

    // USD 1000 minor × 0.9 = 900 EUR minor.
    verify(snapshotRepository)
        .upsert(eq("TWILIO"), eq("ACTUAL"), any(), eq("USD"), eq(1000L), eq(900L), any(), any());
    verify(snapshotRepository)
        .upsert(
            eq("HETZNER"), eq("ESTIMATED"), any(), eq("EUR"), eq(2000L), eq(2000L), any(), any());
    // Only the two available sources are persisted.
    verify(snapshotRepository, times(2))
        .upsert(any(), any(), any(), any(), anyLong(), anyLong(), any(), any());
  }

  @Test
  @DisplayName("overview totals available providers and derives cost-to-serve")
  void overviewTotalsAndInsight() {
    when(snapshotRepository.forMonth(any()))
        .thenReturn(
            List.of(
                new LatestProviderCost(
                    "HETZNER", "ESTIMATED", "EUR", 5000, 5000, LocalDateTime.now(clock))));
    when(snapshotRepository.monthlyTotals(any())).thenReturn(List.of());
    when(aggregateRepository.countActiveTeams()).thenReturn(10L);

    CostOverviewResponse overview = serviceWith().overview();

    assertThat(overview.status()).isEqualTo(PanelStatus.LIVE);
    assertThat(overview.totalMonthlyEurMinor()).isEqualTo(5000);
    assertThat(overview.providers())
        .filteredOn(ProviderCost::available)
        .extracting(ProviderCost::provider)
        .containsExactly("HETZNER");
    // €50.00 / 10 teams = €5.00 cost-to-serve.
    assertThat(overview.insights()).anySatisfy(i -> assertThat(i.value()).contains("5.00"));
  }

  @Test
  @DisplayName("cost watch is PREVIEW until at least one provider has data")
  void costWatchPreviewWhenEmpty() {
    when(snapshotRepository.forMonth(any())).thenReturn(List.of());
    when(snapshotRepository.monthlyTotals(any())).thenReturn(List.of());
    when(aggregateRepository.countActiveTeams()).thenReturn(0L);

    CostWatchResponse watch = serviceWith().costWatch();

    assertThat(watch.status()).isEqualTo(PanelStatus.PREVIEW);
    assertThat(watch.previewCta()).contains("Configure cost providers to enable");
  }
}
