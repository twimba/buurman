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
 * Resolves admin-editable provider cost parameters from {@code cost_config} (set on the backoffice
 * Costs page). The DB is the only source — figures default to 0 (unset) until a Buurmy enters them,
 * with no application.yml/env seed.
 */
@Service
@RequiredArgsConstructor
public class CostConfigService {

  static final String MAILGUN_BASE_EUR = "mailgun.base_eur";
  static final String MAILGUN_PER_EMAIL_EUR = "mailgun.per_email_eur";

  private final CostConfigRepository repository;

  /** Mailgun flat plan fee in EUR ({@code cost_config}, else 0). */
  public double mailgunBaseEur() {
    return doubleValue(repository.all().get(MAILGUN_BASE_EUR), 0);
  }

  /** Mailgun per-accepted-email rate in EUR ({@code cost_config}, else 0). */
  public double mailgunPerEmailEur() {
    return doubleValue(repository.all().get(MAILGUN_PER_EMAIL_EUR), 0);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public CostConfigResponse getConfig() {
    Map<String, String> stored = repository.all();
    return new CostConfigResponse(
        doubleValue(stored.get(MAILGUN_BASE_EUR), 0),
        doubleValue(stored.get(MAILGUN_PER_EMAIL_EUR), 0));
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
