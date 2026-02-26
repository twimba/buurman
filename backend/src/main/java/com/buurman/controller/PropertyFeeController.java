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

import com.buurman.dto.request.CreatePropertyFeeRequest;
import com.buurman.dto.request.UpdatePropertyFeeRequest;
import com.buurman.dto.response.PropertyFeeResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PropertyFeeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/properties/{propertyIdentifier}/financials/fees")
@Tag(name = "Property Fees", description = "Recurring property fees (HOA, management, etc.)")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PropertyFeeController {

  private final PropertyFeeService feeService;

  @Operation(summary = "List fees", description = "Get all fees for a property")
  @GetMapping
  public List<PropertyFeeResponse> listFees(
      @PathVariable String propertyIdentifier, @AuthenticationPrincipal UserPrincipal principal) {
    return feeService.listByProperty(propertyIdentifier, principal);
  }

  @Operation(summary = "Get fee", description = "Get details of a specific fee")
  @GetMapping("/{identifier}")
  public PropertyFeeResponse getFee(
      @PathVariable String propertyIdentifier,
      @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return feeService.get(identifier, principal);
  }

  @Operation(summary = "Create fee", description = "Add a new recurring fee (Admin/Editor)")
  @PostMapping
  @ResponseStatus(CREATED)
  public PropertyFeeResponse createFee(
      @PathVariable String propertyIdentifier,
      @Valid @RequestBody CreatePropertyFeeRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return feeService.create(propertyIdentifier, request, principal);
  }

  @Operation(summary = "Update fee", description = "Update a fee record (Admin/Editor)")
  @PutMapping("/{identifier}")
  public PropertyFeeResponse updateFee(
      @PathVariable String propertyIdentifier,
      @PathVariable String identifier,
      @Valid @RequestBody UpdatePropertyFeeRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return feeService.update(identifier, request, principal);
  }

  @Operation(summary = "Delete fee", description = "Soft delete a fee (Admin/Editor)")
  @DeleteMapping("/{identifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteFee(
      @PathVariable String propertyIdentifier,
      @PathVariable String identifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    feeService.delete(identifier, principal);
  }
}
