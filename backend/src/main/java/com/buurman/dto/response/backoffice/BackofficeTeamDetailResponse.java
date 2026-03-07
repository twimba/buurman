package com.buurman.dto.response.backoffice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.buurman.domain.Sid;

public record BackofficeTeamDetailResponse(
    Sid identifier,
    String teamName,
    Instant createdAt,
    Optional<Instant> updatedAt,
    List<MemberInfo> members,
    DataCounts dataCounts,
    FinancialSnapshot financialSnapshot,
    Optional<SettingsInfo> settings) {
  public record MemberInfo(
      Optional<String> email,
      Optional<String> firstName,
      Optional<String> lastName,
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
      Optional<BigDecimal> totalActiveRent,
      Optional<String> currency,
      Map<String, Long> propertyStatusDistribution,
      Map<String, Long> propertyCategoryDistribution,
      Map<String, Long> contractStatusDistribution,
      Map<String, Long> paymentStatusDistribution) {}

  public record SettingsInfo(
      Optional<Integer> paymentsAheadCount,
      boolean autoGenerationEnabled,
      String defaultCurrency,
      Optional<String> defaultCountryCode,
      Optional<String> timezone,
      Optional<String> dateFormat,
      Optional<String> fiscalYearStartMonth) {}
}
