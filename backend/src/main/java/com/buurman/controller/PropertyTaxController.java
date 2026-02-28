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

import com.buurman.dto.request.CreatePropertyTaxRequest;
import com.buurman.dto.request.UpdatePropertyTaxRequest;
import com.buurman.dto.response.PropertyTaxResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyTaxService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/properties/{propertyIdentifier}/financials/taxes")
@Tag(name = "Property Taxes", description = "Property tax obligations")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PropertyTaxController {

  private final PropertyTaxService taxService;

  @Operation(summary = "List taxes", description = "Get all tax records for a property")
  @GetMapping
  public List<PropertyTaxResponse> listTaxes(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return taxService.listByProperty(propertyIdentifier, principal);
  }

  @Operation(summary = "Get tax", description = "Get details of a specific tax record")
  @GetMapping("/{identifier}")
  public PropertyTaxResponse getTax(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Tax ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return taxService.get(identifier, principal);
  }

  @Operation(summary = "Create tax", description = "Add a new tax obligation (Admin/Editor)")
  @PostMapping
  @ResponseStatus(CREATED)
  public PropertyTaxResponse createTax(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Valid @RequestBody CreatePropertyTaxRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return taxService.create(propertyIdentifier, request, principal);
  }

  @Operation(summary = "Update tax", description = "Update a tax record (Admin/Editor)")
  @PutMapping("/{identifier}")
  public PropertyTaxResponse updateTax(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Tax ULID identifier") @PathVariable String identifier,
      @Valid @RequestBody UpdatePropertyTaxRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return taxService.update(identifier, request, principal);
  }

  @Operation(summary = "Delete tax", description = "Soft delete a tax record (Admin/Editor)")
  @DeleteMapping("/{identifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteTax(
      @Parameter(description = "Property ULID identifier") @PathVariable String propertyIdentifier,
      @Parameter(description = "Tax ULID identifier") @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    taxService.delete(identifier, principal);
  }
}
