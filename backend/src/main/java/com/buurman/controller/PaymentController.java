package com.buurman.controller;

import com.buurman.domain.Payment;
import com.buurman.dto.request.BulkGeneratePaymentsRequest;
import com.buurman.dto.request.CreatePaymentRequest;
import com.buurman.dto.request.MarkPaidRequest;
import com.buurman.dto.request.UpdatePaymentRequest;
import com.buurman.dto.response.PaymentResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Rent payment tracking and management")
@SecurityRequirement(name = "bearer-jwt")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Create payment", description = "Create a new payment record (Admin/Editor)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PaymentResponse createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.createPayment(request, principal);
    }

    @Operation(summary = "Bulk generate payments", description = "Generate payments for all active contracts for a given month (Admin/Editor)")
    @PostMapping("/bulk-generate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public List<PaymentResponse> bulkGeneratePayments(
            @Valid @RequestBody BulkGeneratePaymentsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.bulkGeneratePayments(request, principal);
    }

    @Operation(summary = "List payments", description = "Get all payments with optional filters")
    @GetMapping
    public List<PaymentResponse> getPayments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID contractId,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (contractId != null) {
            return paymentService.getPaymentsByContract(contractId, principal);
        }

        if (status != null) {
            if ("OVERDUE".equalsIgnoreCase(status)) {
                return paymentService.getOverduePayments(principal);
            }
            // Could add other status filters here
        }

        return paymentService.getAllPayments(principal);
    }

    @Operation(summary = "Get overdue payments", description = "Get all overdue payments for the team")
    @GetMapping("/overdue")
    public List<PaymentResponse> getOverduePayments(
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.getOverduePayments(principal);
    }

    @Operation(summary = "Get payment details", description = "Get details of a specific payment")
    @GetMapping("/{id}")
    public PaymentResponse getPayment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.getPayment(id, principal);
    }

    @Operation(summary = "Update payment", description = "Update payment information (Admin/Editor)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PaymentResponse updatePayment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.updatePayment(id, request, principal);
    }

    @Operation(summary = "Mark payment as paid", description = "Mark a payment as paid with payment date (Admin/Editor)")
    @PutMapping("/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public PaymentResponse markPaymentAsPaid(
            @PathVariable UUID id,
            @Valid @RequestBody MarkPaidRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.markPaymentAsPaid(id, request, principal);
    }

    @Operation(summary = "Delete payment", description = "Soft delete a payment (Admin only, cannot delete paid payments)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public void deletePayment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        paymentService.deletePayment(id, principal);
    }
}
