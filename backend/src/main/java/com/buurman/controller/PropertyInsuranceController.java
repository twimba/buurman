package com.buurman.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.dto.request.CreatePropertyInsuranceRequest;
import com.buurman.dto.request.UpdatePropertyInsuranceRequest;
import com.buurman.dto.response.PropertyInsuranceResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyInsuranceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/properties/{propertyIdentifier}/financials/insurances")
@Tag(name = "Property Insurances", description = "Property insurance policies")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PropertyInsuranceController {

  private final PropertyInsuranceService insuranceService;

  @Operation(summary = "List insurances", description = "Get all insurances for a property")
  @GetMapping
  public List<PropertyInsuranceResponse> listInsurances(
      @PathVariable String propertyIdentifier, @AuthenticationPrincipal UserPrincipal principal) {
    return insuranceService.listByProperty(propertyIdentifier, principal);
  }

  @Operation(summary = "Get insurance", description = "Get details of a specific insurance")
  @GetMapping("/{identifier}")
  public PropertyInsuranceResponse getInsurance(
      @PathVariable String propertyIdentifier,
      @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return insuranceService.get(identifier, principal);
  }

  @Operation(
      summary = "Create insurance",
      description = "Add a new insurance policy (Admin/Editor)")
  @PostMapping
  @ResponseStatus(CREATED)
  public PropertyInsuranceResponse createInsurance(
      @PathVariable String propertyIdentifier,
      @Valid @RequestBody CreatePropertyInsuranceRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return insuranceService.create(propertyIdentifier, request, principal);
  }

  @Operation(
      summary = "Update insurance",
      description = "Update an insurance policy (Admin/Editor)")
  @PutMapping("/{identifier}")
  public PropertyInsuranceResponse updateInsurance(
      @PathVariable String propertyIdentifier,
      @PathVariable String identifier,
      @Valid @RequestBody UpdatePropertyInsuranceRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return insuranceService.update(identifier, request, principal);
  }

  @Operation(
      summary = "Delete insurance",
      description = "Soft delete an insurance policy (Admin/Editor)")
  @DeleteMapping("/{identifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteInsurance(
      @PathVariable String propertyIdentifier,
      @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    insuranceService.delete(identifier, principal);
  }
}
