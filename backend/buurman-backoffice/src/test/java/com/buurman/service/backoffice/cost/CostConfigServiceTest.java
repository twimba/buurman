package com.buurman.service.backoffice.cost;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.dto.request.backoffice.cost.UpdateCostConfigRequest;
import com.buurman.dto.response.backoffice.cost.CostConfigResponse;
import com.buurman.repository.backoffice.CostConfigRepository;

@DisplayName("CostConfigService")
class CostConfigServiceTest {

  private final CostConfigRepository repository = mock(CostConfigRepository.class);

  @Test
  @DisplayName("defaults to 0 when no DB value is stored")
  void defaultsToZero() {
    when(repository.all()).thenReturn(Map.of());
    CostConfigService service = new CostConfigService(repository);

    assertThat(service.mailgunBaseEur()).isZero();
    assertThat(service.mailgunPerEmailEur()).isZero();
  }

  @Test
  @DisplayName("reads the stored DB value")
  void readsDbValue() {
    when(repository.all())
        .thenReturn(
            Map.of(
                CostConfigService.MAILGUN_BASE_EUR, "50",
                CostConfigService.MAILGUN_PER_EMAIL_EUR, "0.001"));
    CostConfigService service = new CostConfigService(repository);

    CostConfigResponse config = service.getConfig();
    assertThat(config.mailgunBaseEur()).isEqualTo(50.0);
    assertThat(config.mailgunPerEmailEur()).isEqualTo(0.001);
  }

  @Test
  @DisplayName("update clamps negatives to zero and persists both keys")
  void updateClampsAndPersists() {
    when(repository.all()).thenReturn(Map.of());
    CostConfigService service = new CostConfigService(repository);

    CostConfigResponse saved =
        service.updateConfig(new UpdateCostConfigRequest(-5, 0.002), "admin@buurman.io");

    assertThat(saved.mailgunBaseEur()).isZero();
    assertThat(saved.mailgunPerEmailEur()).isEqualTo(0.002);
    verify(repository).upsert(CostConfigService.MAILGUN_BASE_EUR, "0.0", "admin@buurman.io");
    verify(repository).upsert(CostConfigService.MAILGUN_PER_EMAIL_EUR, "0.002", "admin@buurman.io");
  }
}
