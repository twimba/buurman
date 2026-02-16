package com.buurman.dto.response.backoffice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record BackofficeTeamDetailResponse(
    String identifier,
    String teamName,
    Instant createdAt,
    Instant updatedAt,
    List<MemberInfo> members,
    DataCounts dataCounts,
    FinancialSnapshot financialSnapshot,
    SettingsInfo settings) {
  public record MemberInfo(
      String email,
      String firstName,
      String lastName,
      String role,
      boolean isOwner,
      Instant joinedAt,
      boolean disabled) {}

  public record DataCounts(
      long properties,
      long tenants,
      long contracts,
      long expenses,
      long payments,
      long documents) {}

  public record FinancialSnapshot(
      BigDecimal totalActiveRent,
      String currency,
      Map<String, Long> propertyStatusDistribution,
      Map<String, Long> contractStatusDistribution,
      Map<String, Long> paymentStatusDistribution) {}

  public record SettingsInfo(
      Integer paymentsAheadCount,
      boolean autoGenerationEnabled,
      String defaultCurrency,
      String defaultCountry,
      String timezone,
      String dateFormat,
      String fiscalYearStartMonth) {}
}
