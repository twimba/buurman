package com.buurman.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.List;
import java.util.Optional;

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

import com.buurman.dto.request.CreatePropertyValuationRequest;
import com.buurman.dto.request.UpdatePropertyValuationRequest;
import com.buurman.dto.response.PropertyValuationResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyValuationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/properties/{propertyIdentifier}/financials/valuations")
@Tag(name = "Property Valuations", description = "Property valuation history (time-series)")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PropertyValuationController {

  private final PropertyValuationService valuationService;

  @Operation(summary = "List valuations", description = "Get all valuations for a property")
  @GetMapping
  public List<PropertyValuationResponse> listValuations(
      @PathVariable String propertyIdentifier, @AuthenticationPrincipal UserPrincipal principal) {
    return valuationService.listByProperty(propertyIdentifier, principal);
  }

  @Operation(
      summary = "Get latest valuation",
      description = "Get the most recent valuation for a property")
  @GetMapping("/latest")
  public Optional<PropertyValuationResponse> getLatestValuation(
      @PathVariable String propertyIdentifier, @AuthenticationPrincipal UserPrincipal principal) {
    return valuationService.getLatestByProperty(propertyIdentifier, principal);
  }

  @Operation(
      summary = "Create valuation",
      description = "Add a new valuation record (Admin/Editor)")
  @PostMapping
  @ResponseStatus(CREATED)
  public PropertyValuationResponse createValuation(
      @PathVariable String propertyIdentifier,
      @Valid @RequestBody CreatePropertyValuationRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return valuationService.create(propertyIdentifier, request, principal);
  }

  @Operation(summary = "Update valuation", description = "Update a valuation record (Admin/Editor)")
  @PutMapping("/{identifier}")
  public PropertyValuationResponse updateValuation(
      @PathVariable String propertyIdentifier,
      @PathVariable String identifier,
      @Valid @RequestBody UpdatePropertyValuationRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return valuationService.update(identifier, request, principal);
  }

  @Operation(summary = "Delete valuation", description = "Soft delete a valuation (Admin/Editor)")
  @DeleteMapping("/{identifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteValuation(
      @PathVariable String propertyIdentifier,
      @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    valuationService.delete(identifier, principal);
  }
}
