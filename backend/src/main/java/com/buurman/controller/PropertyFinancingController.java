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

import com.buurman.dto.request.CreateFinancingPaymentRequest;
import com.buurman.dto.request.CreatePropertyFinancingRequest;
import com.buurman.dto.request.UpdateFinancingPaymentRequest;
import com.buurman.dto.request.UpdatePropertyFinancingRequest;
import com.buurman.dto.response.FinancingPaymentResponse;
import com.buurman.dto.response.PropertyFinancingResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.FinancingPaymentService;
import com.buurman.service.PropertyFinancingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/properties/{propertyIdentifier}/financials/financings")
@Tag(
    name = "Property Financings",
    description = "Financing instruments and payments (mortgages, loans, etc.)")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class PropertyFinancingController {

  private final PropertyFinancingService financingService;
  private final FinancingPaymentService paymentService;

  // ===== Financings =====

  @Operation(summary = "List financings", description = "Get all financings for a property")
  @GetMapping
  public List<PropertyFinancingResponse> listFinancings(
      @PathVariable String propertyIdentifier, @AuthenticationPrincipal UserPrincipal principal) {
    return financingService.listByProperty(propertyIdentifier, principal);
  }

  @Operation(summary = "Get financing", description = "Get details of a specific financing")
  @GetMapping("/{financingIdentifier}")
  public PropertyFinancingResponse getFinancing(
      @PathVariable String propertyIdentifier,
      @PathVariable String financingIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return financingService.getFinancing(financingIdentifier, principal);
  }

  @Operation(
      summary = "Create financing",
      description = "Add a new financing instrument (Admin/Editor)")
  @PostMapping
  @ResponseStatus(CREATED)
  public PropertyFinancingResponse createFinancing(
      @PathVariable String propertyIdentifier,
      @Valid @RequestBody CreatePropertyFinancingRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return financingService.create(propertyIdentifier, request, principal);
  }

  @Operation(
      summary = "Update financing",
      description = "Update a financing instrument (Admin/Editor)")
  @PutMapping("/{financingIdentifier}")
  public PropertyFinancingResponse updateFinancing(
      @PathVariable String propertyIdentifier,
      @PathVariable String financingIdentifier,
      @Valid @RequestBody UpdatePropertyFinancingRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return financingService.update(financingIdentifier, request, principal);
  }

  @Operation(
      summary = "Delete financing",
      description = "Soft delete a financing instrument (Admin/Editor)")
  @DeleteMapping("/{financingIdentifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteFinancing(
      @PathVariable String propertyIdentifier,
      @PathVariable String financingIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    financingService.delete(financingIdentifier, principal);
  }

  // ===== Financing Payments =====

  @Operation(summary = "List payments", description = "Get all payments for a financing instrument")
  @GetMapping("/{financingIdentifier}/payments")
  public List<FinancingPaymentResponse> listPayments(
      @PathVariable String propertyIdentifier,
      @PathVariable String financingIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    return paymentService.listByFinancing(financingIdentifier, principal);
  }

  @Operation(
      summary = "Create payment",
      description = "Record a payment for a financing instrument (Admin/Editor)")
  @PostMapping("/{financingIdentifier}/payments")
  @ResponseStatus(CREATED)
  public FinancingPaymentResponse createPayment(
      @PathVariable String propertyIdentifier,
      @PathVariable String financingIdentifier,
      @Valid @RequestBody CreateFinancingPaymentRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return paymentService.create(financingIdentifier, request, principal);
  }

  @Operation(summary = "Update payment", description = "Update a payment record (Admin/Editor)")
  @PutMapping("/{financingIdentifier}/payments/{paymentIdentifier}")
  public FinancingPaymentResponse updatePayment(
      @PathVariable String propertyIdentifier,
      @PathVariable String financingIdentifier,
      @PathVariable String paymentIdentifier,
      @Valid @RequestBody UpdateFinancingPaymentRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return paymentService.update(paymentIdentifier, request, principal);
  }

  @Operation(summary = "Delete payment", description = "Soft delete a payment (Admin/Editor)")
  @DeleteMapping("/{financingIdentifier}/payments/{paymentIdentifier}")
  @ResponseStatus(NO_CONTENT)
  public void deletePayment(
      @PathVariable String propertyIdentifier,
      @PathVariable String financingIdentifier,
      @PathVariable String paymentIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    paymentService.delete(paymentIdentifier, principal);
  }
}
