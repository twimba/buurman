package com.buurman.controller;

import com.buurman.domain.Payment;
import com.buurman.domain.SortDirection;
import com.buurman.dto.request.BulkGeneratePaymentsRequest;
import com.buurman.dto.request.CreatePaymentReceivalRequest;
import com.buurman.dto.request.CreatePaymentRequest;
import com.buurman.dto.request.MarkPaidRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdatePaymentReceivalRequest;
import com.buurman.dto.request.UpdatePaymentRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.PaymentReceivalResponse;
import com.buurman.dto.response.PaymentResponse;
import com.buurman.dto.response.PaymentStatsResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/payments")
@Tag(name = "Payments", description = "Rent payment tracking and management")
@SecurityRequirement(name = "bearer-jwt")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Create payment", description = "Create a new payment record (Admin/Editor)")
    @PostMapping
    @ResponseStatus(CREATED)
    public PaymentResponse createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.createPayment(request, principal);
    }

    @Operation(summary = "Bulk generate payments", description = "Generate payments for all active contracts for a given month (Admin/Editor)")
    @PostMapping("/bulk-generate")
    @ResponseStatus(CREATED)
    public List<PaymentResponse> bulkGeneratePayments(
            @Valid @RequestBody BulkGeneratePaymentsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.bulkGeneratePayments(request, principal);
    }

    @Operation(summary = "List payments", description = "Get all payments with optional filters and pagination")
    @GetMapping
    public PageResponse<PaymentResponse> getPayments(
            @RequestParam(required = false) Payment.PaymentStatus status,
            @RequestParam(required = false) String contractIdentifier,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "25") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "DESC") SortDirection direction,
            @AuthenticationPrincipal UserPrincipal principal) {

        // When filtering by contractIdentifier, use existing per-contract list wrapped in PageResponse
        if (contractIdentifier != null) {
            List<PaymentResponse> results = paymentService.getPaymentsByContract(contractIdentifier, principal);
            return PageResponse.of(results, 0, results.size(), results.size());
        }

        PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
        return paymentService.getPaymentsPaginated(principal, status != null ? status.name() : null, null, pageRequest);
    }

    @Operation(summary = "Get overdue payments", description = "Get all overdue payments for the team")
    @GetMapping("/overdue")
    public List<PaymentResponse> getOverduePayments(
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.getOverduePayments(principal);
    }

    @Operation(summary = "Get payment stats", description = "Get payment statistics for the team")
    @GetMapping("/stats")
    public PaymentStatsResponse getPaymentStats(@AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.getPaymentStats(principal);
    }

    @Operation(summary = "Get payment details", description = "Get details of a specific payment")
    @GetMapping("/{identifier}")
    public PaymentResponse getPayment(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.getPayment(identifier, principal);
    }

    @Operation(summary = "Update payment", description = "Update payment information (Admin/Editor)")
    @PutMapping("/{identifier}")
    public PaymentResponse updatePayment(
            @PathVariable String identifier,
            @Valid @RequestBody UpdatePaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.updatePayment(identifier, request, principal);
    }

    @Operation(summary = "Mark payment as paid", description = "Mark a payment as paid with payment date (Admin/Editor)")
    @PutMapping("/{identifier}/mark-paid")
    public PaymentResponse markPaymentAsPaid(
            @PathVariable String identifier,
            @Valid @RequestBody MarkPaidRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.markPaymentAsPaid(identifier, request, principal);
    }

    // --- Receival endpoints ---

    @Operation(summary = "Register receival", description = "Register a partial or full payment receival (Admin/Editor)")
    @PostMapping("/{identifier}/receivals")
    @ResponseStatus(CREATED)
    public PaymentResponse registerReceival(
            @PathVariable String identifier,
            @Valid @RequestBody CreatePaymentReceivalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.registerReceival(identifier, request, principal);
    }

    @Operation(summary = "List receivals", description = "Get all receivals for a payment")
    @GetMapping("/{identifier}/receivals")
    public List<PaymentReceivalResponse> getReceivals(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.getReceivalsForPayment(identifier, principal);
    }

    @Operation(summary = "Update receival", description = "Update a receival's amount, date, or notes (Admin/Editor)")
    @PutMapping("/{identifier}/receivals/{receivalIdentifier}")
    public PaymentResponse updateReceival(
            @PathVariable String identifier,
            @PathVariable String receivalIdentifier,
            @Valid @RequestBody UpdatePaymentReceivalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.updateReceival(identifier, receivalIdentifier, request, principal);
    }

    @Operation(summary = "Delete receival", description = "Soft delete a receival (Admin/Editor)")
    @DeleteMapping("/{identifier}/receivals/{receivalIdentifier}")
    public PaymentResponse deleteReceival(
            @PathVariable String identifier,
            @PathVariable String receivalIdentifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.deleteReceival(identifier, receivalIdentifier, principal);
    }

    // --- End receival endpoints ---

    @Operation(summary = "Delete payment", description = "Soft delete a payment (Admin only, cannot delete paid payments)")
    @DeleteMapping("/{identifier}")
    @ResponseStatus(NO_CONTENT)
    public void deletePayment(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        paymentService.deletePayment(identifier, principal);
    }

    @Operation(summary = "Upload document", description = "Upload a document for a payment (Admin/Editor)")
    @PostMapping("/{identifier}/documents")
    @ResponseStatus(CREATED)
    public DocumentResponse uploadDocument(
            @PathVariable String identifier,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.uploadDocument(identifier, file, title, notes, principal);
    }

    @Operation(summary = "List documents", description = "Get all documents for a payment")
    @GetMapping("/{identifier}/documents")
    public List<DocumentResponse> getDocuments(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.getDocuments(identifier, principal);
    }

    @Operation(summary = "Get download URL", description = "Get presigned download URL for a document")
    @GetMapping("/documents/{documentIdentifier}/download")
    public Map<String, String> getDownloadUrl(
            @PathVariable String documentIdentifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        URL url = paymentService.getDocumentDownloadUrl(documentIdentifier, principal);
        return Map.of("url", url.toString());
    }

    @Operation(summary = "Delete document", description = "Delete a document (Admin/Editor)")
    @DeleteMapping("/documents/{documentIdentifier}")
    @ResponseStatus(NO_CONTENT)
    public void deleteDocument(
            @PathVariable String documentIdentifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        paymentService.deleteDocument(documentIdentifier, principal);
    }

    @Operation(summary = "Get audit log", description = "Get audit history for a payment")
    @GetMapping("/{identifier}/audit-log")
    public List<RecentActivityResponse> getPaymentAuditLog(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.getAuditLog(identifier, principal);
    }
}
