package com.buurman.controller;

import com.buurman.domain.Contract;
import com.buurman.dto.request.ChangeContractStatusRequest;
import com.buurman.dto.request.CreateContractRequest;
import com.buurman.dto.request.GeneratePaymentsRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdateContractRequest;
import com.buurman.dto.response.ContractResponse;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
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

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.List;
import java.util.Map;

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
    public ContractResponse createContract(
            @Valid @RequestBody CreateContractRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return contractService.createContract(request, principal);
    }

    @Operation(summary = "List contracts", description = "Get all contracts with optional filters and pagination")
    @GetMapping
    public PageResponse<ContractResponse> getContracts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String propertyIdentifier,
            @RequestParam(required = false) String tenantIdentifier,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "25") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal UserPrincipal principal) {

        // When filtering by property or tenant identifier, use the existing non-paginated methods wrapped in PageResponse
        if (propertyIdentifier != null) {
            List<ContractResponse> results = contractService.getContractsByProperty(propertyIdentifier, principal);
            return PageResponse.of(results, 0, results.size(), results.size());
        }

        if (tenantIdentifier != null) {
            List<ContractResponse> results = contractService.getContractsByTenant(tenantIdentifier, principal);
            return PageResponse.of(results, 0, results.size(), results.size());
        }

        PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
        return contractService.getContractsPaginated(principal, status, pageRequest);
    }

    @Operation(summary = "Get contract details", description = "Get details of a specific contract")
    @GetMapping("/{identifier}")
    public ContractResponse getContract(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return contractService.getContract(identifier, principal);
    }

    @Operation(summary = "Update contract", description = "Update contract information (Admin/Editor)")
    @PutMapping("/{identifier}")
    public ContractResponse updateContract(
            @PathVariable String identifier,
            @Valid @RequestBody UpdateContractRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return contractService.updateContract(identifier, request, principal);
    }

    @Operation(summary = "Delete contract", description = "Soft delete a contract (Admin only)")
    @DeleteMapping("/{identifier}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteContract(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        contractService.deleteContract(identifier, principal);
    }

    @Operation(summary = "Change contract status", description = "Change the status of a contract (Admin/Editor)")
    @PostMapping("/{identifier}/change-status")
    public ContractResponse changeContractStatus(
            @PathVariable String identifier,
            @Valid @RequestBody ChangeContractStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return contractService.changeContractStatus(identifier, request, principal);
    }

    @Operation(summary = "Reopen contract", description = "Reopen a terminated or expired contract back to draft status (Admin/Editor)")
    @PostMapping("/{identifier}/reopen")
    public ContractResponse reopenContract(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return contractService.reopenContract(identifier, principal);
    }

    @Operation(summary = "Duplicate contract", description = "Create a new contract with the same data in draft status (Admin/Editor)")
    @PostMapping("/{identifier}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse duplicateContract(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return contractService.duplicateContract(identifier, principal);
    }

    @Operation(summary = "Upload document", description = "Upload a document for a contract (Admin/Editor)")
    @PostMapping("/{identifier}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadDocument(
            @PathVariable String identifier,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserPrincipal principal) {
        return contractService.uploadDocument(identifier, file, title, notes, principal);
    }

    @Operation(summary = "List documents", description = "Get all documents for a contract")
    @GetMapping("/{identifier}/documents")
    public List<DocumentResponse> getDocuments(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return contractService.getDocuments(identifier, principal);
    }

    @Operation(summary = "Get download URL", description = "Get presigned download URL for a document")
    @GetMapping("/documents/{documentIdentifier}/download")
    public Map<String, String> getDownloadUrl(
            @PathVariable String documentIdentifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        URL url = contractService.getDocumentDownloadUrl(documentIdentifier, principal);
        return Map.of("url", url.toString());
    }

    @Operation(summary = "Delete document", description = "Delete a document (Admin/Editor)")
    @DeleteMapping("/documents/{documentIdentifier}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(
            @PathVariable String documentIdentifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        contractService.deleteDocument(documentIdentifier, principal);
    }

    @Operation(summary = "Get audit log", description = "Get audit history for a contract")
    @GetMapping("/{identifier}/audit-log")
    public List<RecentActivityResponse> getContractAuditLog(
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        return contractService.getAuditLog(identifier, principal);
    }

    @Operation(summary = "Generate payments", description = "Manually generate N future payments for a contract (Admin/Editor)")
    @PostMapping("/{identifier}/generate-payments")
    public Map<String, Object> generatePayments(
            @PathVariable String identifier,
            @Valid @RequestBody GeneratePaymentsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return contractService.generatePayments(identifier, request, principal);
    }
}
