package com.buurman.controller.backoffice;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.PhoneNumberPolicy;
import com.buurman.domain.RateLimitConfig;
import com.buurman.dto.request.backoffice.UpdatePhoneNumberPolicyRequest;
import com.buurman.dto.request.backoffice.UpdateRateLimitConfigRequest;
import com.buurman.dto.response.backoffice.BackofficePhoneNumberPolicyResponse;
import com.buurman.dto.response.backoffice.CountryEntry;
import com.buurman.dto.response.backoffice.CountryGroupResponse;
import com.buurman.dto.response.backoffice.RateLimitConfigResponse;
import com.buurman.generated.backoffice.api.BackofficeSettingsApi;
import com.buurman.security.BackofficePrincipal;
import com.buurman.security.SecurityUtils;
import com.buurman.service.PhoneNumberPolicyService;
import com.buurman.service.RateLimitConfigService;
import com.buurman.util.CountryGroups;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BackofficeSettingsController implements BackofficeSettingsApi {

  private final PhoneNumberPolicyService policyService;
  private final RateLimitConfigService rateLimitConfigService;

  @Override
  public BackofficePhoneNumberPolicyResponse getPhonePolicy() {
    PhoneNumberPolicy policy = policyService.getPolicy();
    return toResponse(policy);
  }

  @Override
  public BackofficePhoneNumberPolicyResponse updatePhonePolicy(
      UpdatePhoneNumberPolicyRequest updatePhoneNumberPolicyRequest) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    PhoneNumberPolicy policy = policyService.getPolicy();
    policy.setPolicyMatrix(Optional.ofNullable(updatePhoneNumberPolicyRequest.policyMatrix()));
    policy.setMaxCodesPerHour(updatePhoneNumberPolicyRequest.maxCodesPerHour());
    policy.setVerificationCodeExpiryMinutes(
        updatePhoneNumberPolicyRequest.verificationCodeExpiryMinutes());
    policy.setUpdatedBy(principal.getEmail());

    policy = policyService.updatePolicy(policy);
    return toResponse(policy);
  }

  @Override
  public Map<String, Object> getPhonePolicyMetadata() {
    List<CountryGroupResponse> groups =
        CountryGroups.GROUPS.stream()
            .map(
                g ->
                    new CountryGroupResponse(
                        g.id(),
                        g.name(),
                        g.countries().stream()
                            .map(c -> new CountryEntry(c.code(), c.name()))
                            .toList()))
            .toList();

    return Map.of("countryGroups", groups, "numberTypes", CountryGroups.ALL_NUMBER_TYPES);
  }

  // -- Rate Limit Config --

  @Override
  public List<RateLimitConfigResponse> listRateLimits() {
    return rateLimitConfigService.getAllConfigs().stream().map(this::toRateLimitResponse).toList();
  }

  @Override
  public RateLimitConfigResponse getRateLimit(String key) {
    RateLimitConfig config =
        rateLimitConfigService
            .getConfig(key)
            .orElseThrow(
                () ->
                    new com.buurman.exception.NotFoundException(
                        "Rate limit config not found: " + key));
    return toRateLimitResponse(config);
  }

  @Override
  public RateLimitConfigResponse updateRateLimit(
      String key, UpdateRateLimitConfigRequest updateRateLimitConfigRequest) {
    BackofficePrincipal principal = SecurityUtils.getBackofficePrincipal();
    RateLimitConfig updated =
        rateLimitConfigService.updateConfig(
            key,
            updateRateLimitConfigRequest.maxRequests(),
            updateRateLimitConfigRequest.periodSeconds(),
            updateRateLimitConfigRequest.enabled(),
            principal.getEmail().orElse("unknown"));
    return toRateLimitResponse(updated);
  }

  private RateLimitConfigResponse toRateLimitResponse(RateLimitConfig config) {
    return new RateLimitConfigResponse(
        config.getKey(),
        config.getDisplayName(),
        config.getDescription(),
        config.getMaxRequests(),
        config.getPeriodSeconds(),
        config.isEnabled(),
        config.getUpdatedAt(),
        config.getUpdatedBy());
  }

  // -- Helpers --

  private BackofficePhoneNumberPolicyResponse toResponse(PhoneNumberPolicy policy) {
    Map<String, List<String>> matrix = policy.getPolicyMatrix().orElse(Map.of());
    return new BackofficePhoneNumberPolicyResponse(
        matrix,
        policy.getMaxCodesPerHour(),
        policy.getVerificationCodeExpiryMinutes(),
        policy.getUpdatedAt(),
        policy.getUpdatedBy());
  }
}
