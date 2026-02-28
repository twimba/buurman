package com.buurman.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.response.PropertyFinancialSummaryResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyFinancialsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/properties/{propertyIdentifier}/financials")
@Tag(name = "Property Financials", description = "Aggregated property financial overview")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PropertyFinancialsController {

  private final PropertyFinancialsService financialsService;

  @Operation(
      summary = "Get financial summary",
      description = "Get aggregated financial summary for a property")
  @GetMapping
  public PropertyFinancialSummaryResponse getFinancialSummary(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return financialsService.getFinancialSummary(propertyIdentifier, principal);
  }
}
