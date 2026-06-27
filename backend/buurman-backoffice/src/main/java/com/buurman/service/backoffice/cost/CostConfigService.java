package com.buurman.service.backoffice.cost;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.dto.request.backoffice.cost.UpdateCostConfigRequest;
import com.buurman.dto.response.backoffice.cost.CostConfigResponse;
import com.buurman.repository.backoffice.CostConfigRepository;

import lombok.RequiredArgsConstructor;

/**
 * Resolves admin-editable provider cost parameters: the value stored in {@code cost_config} (set
 * from the backoffice Costs page) takes precedence over the application.yml seed. Lets a Buurmy
 * manage figures like the Mailgun plan fee without a redeploy.
 */
@Service
@RequiredArgsConstructor
public class CostConfigService {

  static final String MAILGUN_BASE_EUR = "mailgun.base_eur";
  static final String MAILGUN_PER_EMAIL_EUR = "mailgun.per_email_eur";

  private final CostConfigRepository repository;
  private final CostProperties props;

  /** Mailgun flat plan fee in EUR (DB override, else application.yml seed). */
  public double mailgunBaseEur() {
    return doubleValue(repository.all().get(MAILGUN_BASE_EUR), props.mailgun().baseEur());
  }

  /** Mailgun per-accepted-email rate in EUR (DB override, else application.yml seed). */
  public double mailgunPerEmailEur() {
    return doubleValue(repository.all().get(MAILGUN_PER_EMAIL_EUR), props.mailgun().perEmailEur());
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public CostConfigResponse getConfig() {
    Map<String, String> stored = repository.all();
    return new CostConfigResponse(
        doubleValue(stored.get(MAILGUN_BASE_EUR), props.mailgun().baseEur()),
        doubleValue(stored.get(MAILGUN_PER_EMAIL_EUR), props.mailgun().perEmailEur()));
  }

  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public CostConfigResponse updateConfig(UpdateCostConfigRequest request, String updatedBy) {
    repository.upsert(
        MAILGUN_BASE_EUR, Double.toString(nonNegative(request.mailgunBaseEur())), updatedBy);
    repository.upsert(
        MAILGUN_PER_EMAIL_EUR,
        Double.toString(nonNegative(request.mailgunPerEmailEur())),
        updatedBy);
    return new CostConfigResponse(
        nonNegative(request.mailgunBaseEur()), nonNegative(request.mailgunPerEmailEur()));
  }

  private static double nonNegative(double value) {
    return Math.max(0, value);
  }

  private static double doubleValue(String stored, double fallback) {
    if (stored == null || stored.isBlank()) {
      return fallback;
    }
    try {
      return Double.parseDouble(stored);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}
