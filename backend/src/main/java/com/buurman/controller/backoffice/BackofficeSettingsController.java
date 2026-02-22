package com.buurman.controller.backoffice;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.PhoneNumberPolicy;
import com.buurman.dto.request.backoffice.UpdatePhoneNumberPolicyRequest;
import com.buurman.dto.response.backoffice.BackofficePhoneNumberPolicyResponse;
import com.buurman.dto.response.backoffice.CountryEntry;
import com.buurman.dto.response.backoffice.CountryGroupResponse;
import com.buurman.security.BackofficePrincipal;
import com.buurman.service.PhoneNumberPolicyService;
import com.buurman.util.CountryGroups;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/backoffice/settings")
@Tag(name = "Backoffice - Settings", description = "Platform settings management")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class BackofficeSettingsController {

  private final PhoneNumberPolicyService policyService;

  @Operation(
      summary = "Get phone number policy",
      description = "Get the current phone number policy configuration")
  @GetMapping("/phone-policy")
  public BackofficePhoneNumberPolicyResponse getPhonePolicy() {
    PhoneNumberPolicy policy = policyService.getPolicy();
    return toResponse(policy);
  }

  @Operation(
      summary = "Update phone number policy",
      description = "Update the phone number policy configuration")
  @PutMapping("/phone-policy")
  public BackofficePhoneNumberPolicyResponse updatePhonePolicy(
      @Valid @RequestBody UpdatePhoneNumberPolicyRequest request,
      @AuthenticationPrincipal BackofficePrincipal principal) {
    PhoneNumberPolicy policy = policyService.getPolicy();
    policy.setPolicyMatrix(request.policyMatrix());
    policy.setMaxCodesPerHour(request.maxCodesPerHour());
    policy.setVerificationCodeExpiryMinutes(request.verificationCodeExpiryMinutes());
    policy.setUpdatedBy(principal.getEmail());

    policy = policyService.updatePolicy(policy);
    return toResponse(policy);
  }

  @Operation(
      summary = "Get phone policy metadata",
      description = "Get static country groups and number types for UI rendering")
  @GetMapping("/phone-policy/metadata")
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

  private BackofficePhoneNumberPolicyResponse toResponse(PhoneNumberPolicy policy) {
    Map<String, List<String>> matrix =
        policy.getPolicyMatrix() != null ? policy.getPolicyMatrix() : Map.of();
    return new BackofficePhoneNumberPolicyResponse(
        matrix,
        policy.getMaxCodesPerHour(),
        policy.getVerificationCodeExpiryMinutes(),
        policy.getUpdatedAt(),
        policy.getUpdatedBy());
  }
}
