package com.buurman.controller;

import com.buurman.dto.request.BulkGeneratePaymentsRequest;
import com.buurman.dto.request.CreatePaymentReceivalRequest;
import com.buurman.dto.request.CreatePaymentRequest;
import com.buurman.dto.request.MarkPaidRequest;
import com.buurman.dto.request.UpdatePaymentReceivalRequest;
import com.buurman.dto.request.UpdatePaymentRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PaymentReceivalResponse;
import com.buurman.dto.response.PaymentResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.AuditService;
import com.buurman.service.DocumentService;
import com.buurman.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Rent payment tracking and management")
@SecurityRequirement(name = "bearer-jwt")
public class PaymentController {

    private final PaymentService paymentService;
    private final DocumentService documentService;
    private final AuditService auditService;

    public PaymentController(PaymentService paymentService, DocumentService documentService, AuditService auditService) {
        this.paymentService = paymentService;
        this.documentService = documentService;
        this.auditService = auditService;
    }

    @Operation(summary = "Create payment", description = "Create a new payment record (Admin/Editor)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.createPayment(request, principal);
    }

    @Operation(summary = "Bulk generate payments", description = "Generate payments for all active contracts for a given month (Admin/Editor)")
    @PostMapping("/bulk-generate")
    @ResponseStatus(HttpStatus.CREATED)
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
    public PaymentResponse updatePayment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.updatePayment(id, request, principal);
    }

    @Operation(summary = "Mark payment as paid", description = "Mark a payment as paid with payment date (Admin/Editor)")
    @PutMapping("/{id}/mark-paid")
    public PaymentResponse markPaymentAsPaid(
            @PathVariable UUID id,
            @Valid @RequestBody MarkPaidRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.markPaymentAsPaid(id, request, principal);
    }

    // --- Receival endpoints ---

    @Operation(summary = "Register receival", description = "Register a partial or full payment receival (Admin/Editor)")
    @PostMapping("/{id}/receivals")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse registerReceival(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePaymentReceivalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.registerReceival(id, request, principal);
    }

    @Operation(summary = "List receivals", description = "Get all receivals for a payment")
    @GetMapping("/{id}/receivals")
    public List<PaymentReceivalResponse> getReceivals(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.getReceivalsForPayment(id, principal);
    }

    @Operation(summary = "Update receival", description = "Update a receival's amount, date, or notes (Admin/Editor)")
    @PutMapping("/{id}/receivals/{receivalId}")
    public PaymentResponse updateReceival(
            @PathVariable UUID id,
            @PathVariable UUID receivalId,
            @Valid @RequestBody UpdatePaymentReceivalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.updateReceival(id, receivalId, request, principal);
    }

    @Operation(summary = "Delete receival", description = "Soft delete a receival (Admin/Editor)")
    @DeleteMapping("/{id}/receivals/{receivalId}")
    public PaymentResponse deleteReceival(
            @PathVariable UUID id,
            @PathVariable UUID receivalId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.deleteReceival(id, receivalId, principal);
    }

    // --- End receival endpoints ---

    @Operation(summary = "Delete payment", description = "Soft delete a payment (Admin only, cannot delete paid payments)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        paymentService.deletePayment(id, principal);
    }

    @Operation(summary = "Upload document", description = "Upload a document for a payment (Admin/Editor)")
    @PostMapping("/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadDocument(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserPrincipal principal) {
        return documentService.uploadDocument(file, "PAYMENT", id, title, notes, principal);
    }

    @Operation(summary = "List documents", description = "Get all documents for a payment")
    @GetMapping("/{id}/documents")
    public List<DocumentResponse> getDocuments(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return documentService.getDocuments("PAYMENT", id, principal);
    }

    @Operation(summary = "Get download URL", description = "Get presigned download URL for a document")
    @GetMapping("/documents/{documentId}/download")
    public Map<String, String> getDownloadUrl(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        URL url = documentService.getDownloadUrl(documentId, principal);
        return Map.of("url", url.toString());
    }

    @Operation(summary = "Delete document", description = "Delete a document (Admin/Editor)")
    @DeleteMapping("/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        documentService.deleteDocument(documentId, principal);
    }

    @Operation(summary = "Get audit log", description = "Get audit history for a payment")
    @GetMapping("/{id}/audit-log")
    public List<RecentActivityResponse> getPaymentAuditLog(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return auditService.getEntityAuditLog(principal.getTeamId(), "PAYMENT", id);
    }
}
