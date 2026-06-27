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

  private static CostProperties propsWithMailgunSeed(double base, double perEmail) {
    return new CostProperties(
        "EUR", null, null, new CostProperties.Mailgun(base, perEmail), Map.of(), null);
  }

  @Test
  @DisplayName("falls back to the application.yml seed when no DB value is stored")
  void seedFallback() {
    when(repository.all()).thenReturn(Map.of());
    CostConfigService service =
        new CostConfigService(repository, propsWithMailgunSeed(35.0, 0.0008));

    assertThat(service.mailgunBaseEur()).isEqualTo(35.0);
    assertThat(service.mailgunPerEmailEur()).isEqualTo(0.0008);
  }

  @Test
  @DisplayName("DB value overrides the seed")
  void dbOverridesSeed() {
    when(repository.all())
        .thenReturn(
            Map.of(
                CostConfigService.MAILGUN_BASE_EUR, "50",
                CostConfigService.MAILGUN_PER_EMAIL_EUR, "0.001"));
    CostConfigService service =
        new CostConfigService(repository, propsWithMailgunSeed(35.0, 0.0008));

    CostConfigResponse config = service.getConfig();
    assertThat(config.mailgunBaseEur()).isEqualTo(50.0);
    assertThat(config.mailgunPerEmailEur()).isEqualTo(0.001);
  }

  @Test
  @DisplayName("update clamps negatives to zero and persists both keys")
  void updateClampsAndPersists() {
    when(repository.all()).thenReturn(Map.of());
    CostConfigService service =
        new CostConfigService(repository, propsWithMailgunSeed(0, 0));

    CostConfigResponse saved =
        service.updateConfig(new UpdateCostConfigRequest(-5, 0.002), "admin@buurman.io");

    assertThat(saved.mailgunBaseEur()).isZero();
    assertThat(saved.mailgunPerEmailEur()).isEqualTo(0.002);
    verify(repository)
        .upsert(CostConfigService.MAILGUN_BASE_EUR, "0.0", "admin@buurman.io");
    verify(repository)
        .upsert(CostConfigService.MAILGUN_PER_EMAIL_EUR, "0.002", "admin@buurman.io");
  }
}
