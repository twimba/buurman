package com.buurman.controller;

import com.buurman.domain.Contract;
import com.buurman.dto.request.ChangeContractStatusRequest;
import com.buurman.dto.request.CreateContractRequest;
import com.buurman.dto.request.GeneratePaymentsRequest;
import com.buurman.dto.request.UpdateContractRequest;
import com.buurman.dto.response.ContractResponse;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.AuditService;
import com.buurman.service.ContractService;
import com.buurman.service.DocumentService;
import com.buurman.service.PaymentSchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
@Tag(name = "Contracts", description = "Rental agreement management")
@SecurityRequirement(name = "bearer-jwt")
public class ContractController {

    private final ContractService contractService;
    private final DocumentService documentService;
    private final AuditService auditService;
    private final PaymentSchedulingService paymentSchedulingService;

    public ContractController(ContractService contractService, DocumentService documentService,
                             AuditService auditService, PaymentSchedulingService paymentSchedulingService) {
        this.contractService = contractService;
        this.documentService = documentService;
        this.auditService = auditService;
        this.paymentSchedulingService = paymentSchedulingService;
    }

    @Operation(summary = "Create contract", description = "Create a new rental agreement (Admin/Editor)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public ContractResponse createContract(
            @Valid @RequestBody CreateContractRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return contractService.createContract(request, principal);
    }

    @Operation(summary = "List contracts", description = "Get all contracts with optional filters")
    @GetMapping
    public List<ContractResponse> getContracts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {

        if (propertyId != null) {
            return contractService.getContractsByProperty(propertyId, principal);
        }

        if (tenantId != null) {
            return contractService.getContractsByTenant(tenantId, principal);
        }

        if (status != null) {
            try {
                Contract.ContractStatus contractStatus = Contract.ContractStatus.valueOf(status.toUpperCase());
                return contractService.getContractsByStatus(contractStatus, principal);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status: " + status);
            }
        }

        return contractService.getAllContracts(principal);
    }

    @Operation(summary = "Get contract details", description = "Get details of a specific contract")
    @GetMapping("/{id}")
    public ContractResponse getContract(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return contractService.getContract(id, principal);
    }

    @Operation(summary = "Update contract", description = "Update contract information (Admin/Editor)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public ContractResponse updateContract(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateContractRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return contractService.updateContract(id, request, principal);
    }

    @Operation(summary = "Delete contract", description = "Soft delete a contract (Admin only)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public void deleteContract(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        contractService.deleteContract(id, principal);
    }

    @Operation(summary = "Change contract status", description = "Change the status of a contract (Admin/Editor)")
    @PostMapping("/{id}/change-status")
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public ContractResponse changeContractStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeContractStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return contractService.changeContractStatus(id, request, principal);
    }

    @Operation(summary = "Reopen contract", description = "Reopen a terminated or expired contract back to draft status (Admin/Editor)")
    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public ContractResponse reopenContract(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return contractService.reopenContract(id, principal);
    }

    @Operation(summary = "Duplicate contract", description = "Create a new contract with the same data in draft status (Admin/Editor)")
    @PostMapping("/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public ContractResponse duplicateContract(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return contractService.duplicateContract(id, principal);
    }

    @Operation(summary = "Upload document", description = "Upload a document for a contract (Admin/Editor)")
    @PostMapping("/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public DocumentResponse uploadDocument(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserPrincipal principal) {
        return documentService.uploadDocument(file, "CONTRACT", id, title, notes, principal);
    }

    @Operation(summary = "List documents", description = "Get all documents for a contract")
    @GetMapping("/{id}/documents")
    public List<DocumentResponse> getDocuments(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return documentService.getDocuments("CONTRACT", id, principal);
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
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public void deleteDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        documentService.deleteDocument(documentId, principal);
    }

    @Operation(summary = "Get audit log", description = "Get audit history for a contract")
    @GetMapping("/{id}/audit-log")
    public List<RecentActivityResponse> getContractAuditLog(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return auditService.getEntityAuditLog(principal.getTeamId(), "CONTRACT", id);
    }

    @Operation(summary = "Generate payments", description = "Manually generate N future payments for a contract (Admin/Editor)")
    @PostMapping("/{id}/generate-payments")
    @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
    public Map<String, Object> generatePayments(
            @PathVariable UUID id,
            @Valid @RequestBody GeneratePaymentsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        int generated = paymentSchedulingService.generatePaymentsManually(
                id,
                principal.getTeamId(),
                principal.getUserId(),
                request.count()
        );
        return Map.of(
                "generated", generated,
                "requested", request.count()
        );
    }
}
