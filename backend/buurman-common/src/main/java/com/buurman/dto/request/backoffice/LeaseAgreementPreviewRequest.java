package com.buurman.dto.request.backoffice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.Contract.ContractType;
import com.buurman.domain.Contract.PaymentFrequency;
import com.buurman.domain.LeaseKind;
import com.buurman.domain.RentComponentType;

/**
 * Backoffice preview of a country lease agreement rendered from synthetic sample values. Nothing
 * here refers to a real contract; every field is validated by the preview service.
 *
 * @param countryCode ISO 3166-1 alpha-2, upper case
 * @param leaseKind any kind, {@code LEGACY} (the placeholder example text) included
 * @param language one of the 13 document languages
 * @param clauses the admin's clause choices; omitted keys keep the template default
 */
public record LeaseAgreementPreviewRequest(
    String countryCode,
    LeaseKind leaseKind,
    String language,
    Sample sample,
    @Nullable List<ClauseChoice> clauses) {

  /** The made-up contract values the document is rendered from. */
  public record Sample(
      ContractType contractType,
      LocalDate startDate,
      @Nullable LocalDate endDate,
      List<RentComponent> rentComponents,
      @Nullable Money deposit,
      @Nullable Integer paymentDueDay,
      @Nullable PaymentFrequency paymentFrequency,
      String landlordName,
      @Nullable List<String> tenantNames,
      String propertyAddress,
      @Nullable String regionCode) {}

  public record RentComponent(RentComponentType type, BigDecimal amount, String currency) {}

  public record Money(BigDecimal amount, String currency) {}

  public record ClauseChoice(String clauseKey, boolean included, int sortOrder) {}
}
