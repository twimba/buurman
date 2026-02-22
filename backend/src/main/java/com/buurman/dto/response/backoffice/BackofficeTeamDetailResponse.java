package com.buurman.dto.response.backoffice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

public record BackofficeTeamDetailResponse(
    String identifier,
    String teamName,
    Instant createdAt,
    @Nullable Instant updatedAt,
    List<MemberInfo> members,
    DataCounts dataCounts,
    FinancialSnapshot financialSnapshot,
    @Nullable SettingsInfo settings) {
  public record MemberInfo(
      @Nullable String email,
      @Nullable String firstName,
      @Nullable String lastName,
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
      @Nullable BigDecimal totalActiveRent,
      @Nullable String currency,
      Map<String, Long> propertyStatusDistribution,
      Map<String, Long> propertyCategoryDistribution,
      Map<String, Long> contractStatusDistribution,
      Map<String, Long> paymentStatusDistribution) {}

  public record SettingsInfo(
      @Nullable Integer paymentsAheadCount,
      boolean autoGenerationEnabled,
      @Nullable String defaultCurrency,
      @Nullable String defaultCountry,
      @Nullable String timezone,
      @Nullable String dateFormat,
      @Nullable String fiscalYearStartMonth) {}
}
