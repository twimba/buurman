package com.buurman.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.domain.Contract.ContractStatus;
import com.buurman.domain.SortDirection;
import com.buurman.dto.request.AddContractPartyRequest;
import com.buurman.dto.request.ChangeContractStatusRequest;
import com.buurman.dto.request.ChangePrimaryTenantRequest;
import com.buurman.dto.request.CreateContractRequest;
import com.buurman.dto.request.GeneratePaymentsRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.request.UpdateContractRequest;
import com.buurman.dto.response.ContractPartyResponse;
import com.buurman.dto.response.ContractResponse;
import com.buurman.dto.response.CountryMetadataSchemaResponse;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.security.UserPrincipal;
import com.buurman.service.AuditService;
import com.buurman.service.ContractPartyService;
import com.buurman.service.ContractService;
import com.buurman.service.CountryMetadataSchemaService;
import com.buurman.service.DocumentService;
import com.buurman.service.PaymentSchedulingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/contracts")
@Tag(name = "Contracts", description = "Rental agreement management")
@SecurityRequirement(name = "bearer-jwt")
@RequiredArgsConstructor
public class ContractController {

  private final ContractService contractService;
  private final ContractPartyService contractPartyService;
  private final CountryMetadataSchemaService countryMetadataSchemaService;
  private final DocumentService documentService;
  private final AuditService auditService;
  private final PaymentSchedulingService paymentSchedulingService;

  @Operation(
      summary = "Create contract",
      description = "Create a new rental agreement (Admin/Editor)")
  @PostMapping
  @ResponseStatus(CREATED)
  public ContractResponse createContract(
      @Valid @RequestBody CreateContractRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return contractService.createContract(request, principal);
  }

  @Operation(
      summary = "List contracts",
      description = "Get all contracts with optional filters and pagination")
  @GetMapping
  public PageResponse<ContractResponse> getContracts(
      @RequestParam Optional<ContractStatus> status,
      @RequestParam Optional<String> propertyIdentifier,
      @RequestParam Optional<String> tenantIdentifier,
      @RequestParam(defaultValue = "0") Integer page,
      @RequestParam(defaultValue = "25") Integer size,
      @RequestParam Optional<String> sort,
      @RequestParam(defaultValue = "DESC") SortDirection direction,
      @AuthenticationPrincipal UserPrincipal principal) {

    // When filtering by property or tenant identifier, use the existing non-paginated methods
    // wrapped in PageResponse
    if (propertyIdentifier.isPresent()) {
      List<ContractResponse> results =
          contractService.getContractsByProperty(propertyIdentifier.get(), principal);
      return PageResponse.of(results, 0, results.size(), results.size());
    }

    if (tenantIdentifier.isPresent()) {
      List<ContractResponse> results =
          contractService.getContractsByTenant(tenantIdentifier.get(), principal);
      return PageResponse.of(results, 0, results.size(), results.size());
    }

    PageRequest pageRequest = PageRequest.of(page, size, sort.orElse(null), direction);
    return contractService.getContractsPaginated(
        principal, status.map(ContractStatus::name).orElse(null), pageRequest);
  }

  @Operation(summary = "Get contract details", description = "Get details of a specific contract")
  @GetMapping("/{identifier}")
  public ContractResponse getContract(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    return contractService.getContract(identifier, principal);
  }

  @Operation(
      summary = "Update contract",
      description = "Update contract information (Admin/Editor)")
  @PutMapping("/{identifier}")
  public ContractResponse updateContract(
      @PathVariable String identifier,
      @Valid @RequestBody UpdateContractRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return contractService.updateContract(identifier, request, principal);
  }

  @Operation(summary = "Delete contract", description = "Soft delete a contract (Admin only)")
  @DeleteMapping("/{identifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteContract(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    contractService.deleteContract(identifier, principal);
  }

  @Operation(
      summary = "Change contract status",
      description = "Change the status of a contract (Admin/Editor)")
  @PostMapping("/{identifier}/change-status")
  public ContractResponse changeContractStatus(
      @PathVariable String identifier,
      @Valid @RequestBody ChangeContractStatusRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return contractService.changeContractStatus(identifier, request, principal);
  }

  @Operation(
      summary = "Reopen contract",
      description = "Reopen a terminated or expired contract back to draft status (Admin/Editor)")
  @PostMapping("/{identifier}/reopen")
  public ContractResponse reopenContract(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    return contractService.reopenContract(identifier, principal);
  }

  @Operation(
      summary = "Duplicate contract",
      description = "Create a new contract with the same data in draft status (Admin/Editor)")
  @PostMapping("/{identifier}/duplicate")
  @ResponseStatus(CREATED)
  public ContractResponse duplicateContract(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    return contractService.duplicateContract(identifier, principal);
  }

  @Operation(
      summary = "Get metadata schema",
      description = "Get country-specific metadata field definitions for dynamic form rendering")
  @GetMapping("/metadata-schema/{countryCode}")
  public CountryMetadataSchemaResponse getMetadataSchema(
      @PathVariable String countryCode, @AuthenticationPrincipal UserPrincipal principal) {
    String normalized =
        com.buurman.domain.metadata.CountryMetadataRegistry.normalizeCountryCode(countryCode);
    if (normalized == null
        || !com.buurman.domain.metadata.CountryMetadataRegistry.getSupportedCountries()
            .contains(normalized)) {
      throw new com.buurman.exception.BadRequestException(
          "Unsupported country code: " + countryCode);
    }
    return countryMetadataSchemaService.getSchema(normalized);
  }

  // --- Contract Party endpoints ---

  @Operation(
      summary = "Add party",
      description = "Add a party (guarantor, cosigner, extra tenant) to a contract (Admin/Editor)")
  @PostMapping("/{identifier}/parties")
  @ResponseStatus(CREATED)
  public ContractPartyResponse addParty(
      @PathVariable String identifier,
      @Valid @RequestBody AddContractPartyRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return contractPartyService.addParty(identifier, request, principal);
  }

  @Operation(
      summary = "Remove party",
      description = "Remove a party from a contract (Admin/Editor)")
  @DeleteMapping("/{identifier}/parties/{partyIdentifier}")
  @ResponseStatus(NO_CONTENT)
  public void removeParty(
      @PathVariable String identifier,
      @PathVariable String partyIdentifier,
      @AuthenticationPrincipal UserPrincipal principal) {
    contractPartyService.removeParty(identifier, partyIdentifier, principal);
  }

  @Operation(
      summary = "Change primary tenant",
      description = "Change the primary tenant of a contract (Admin/Editor)")
  @PostMapping("/{identifier}/parties/change-primary")
  public ContractPartyResponse changePrimaryTenant(
      @PathVariable String identifier,
      @Valid @RequestBody ChangePrimaryTenantRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return contractPartyService.changePrimaryTenant(identifier, request, principal);
  }

  // --- Document endpoints ---

  @Operation(
      summary = "Upload document",
      description = "Upload a document for a contract (Admin/Editor)")
  @PostMapping("/{identifier}/documents")
  @ResponseStatus(CREATED)
  public DocumentResponse uploadDocument(
      @PathVariable String identifier,
      @RequestParam("file") MultipartFile file,
      @RequestParam Optional<String> title,
      @RequestParam Optional<String> notes,
      @AuthenticationPrincipal UserPrincipal principal) {
    return contractService.uploadDocument(
        identifier, file, title.orElse(null), notes.orElse(null), principal);
  }

  @Operation(summary = "List documents", description = "Get all documents for a contract")
  @GetMapping("/{identifier}/documents")
  public List<DocumentResponse> getDocuments(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    return contractService.getDocuments(identifier, principal);
  }

  @Operation(
      summary = "Get download URL",
      description = "Get presigned download URL for a document")
  @GetMapping("/documents/{documentIdentifier}/download")
  public Map<String, String> getDownloadUrl(
      @PathVariable String documentIdentifier, @AuthenticationPrincipal UserPrincipal principal) {
    URL url = contractService.getDocumentDownloadUrl(documentIdentifier, principal);
    return Map.of("url", url.toString());
  }

  @Operation(summary = "Delete document", description = "Delete a document (Admin/Editor)")
  @DeleteMapping("/documents/{documentIdentifier}")
  @ResponseStatus(NO_CONTENT)
  public void deleteDocument(
      @PathVariable String documentIdentifier, @AuthenticationPrincipal UserPrincipal principal) {
    contractService.deleteDocument(documentIdentifier, principal);
  }

  @Operation(summary = "Get audit log", description = "Get audit history for a contract")
  @GetMapping("/{identifier}/audit-log")
  public List<RecentActivityResponse> getContractAuditLog(
      @PathVariable String identifier, @AuthenticationPrincipal UserPrincipal principal) {
    return contractService.getAuditLog(identifier, principal);
  }

  @Operation(
      summary = "Generate payments",
      description = "Manually generate N future payments for a contract (Admin/Editor)")
  @PostMapping("/{identifier}/generate-payments")
  public Map<String, Object> generatePayments(
      @PathVariable String identifier,
      @Valid @RequestBody GeneratePaymentsRequest request,
      @AuthenticationPrincipal UserPrincipal principal) {
    return contractService.generatePayments(identifier, request, principal);
  }
}
