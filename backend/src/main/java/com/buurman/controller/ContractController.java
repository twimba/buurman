package com.buurman.controller;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.ContractPartyIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.TenantIdentifier;
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
import com.buurman.generated.api.ContractsApi;
import com.buurman.generated.model.UploadPhotoRequest;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ContractPartyService;
import com.buurman.service.ContractService;
import com.buurman.service.CountryMetadataSchemaService;
import com.buurman.service.PaymentSchedulingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ContractController implements ContractsApi {

  private final ContractService contractService;
  private final ContractPartyService contractPartyService;
  private final CountryMetadataSchemaService countryMetadataSchemaService;
  private final PaymentSchedulingService paymentSchedulingService;

  @Override
  public ContractResponse createContract(CreateContractRequest createContractRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.createContract(createContractRequest, principal);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public PageResponse getContracts(
      String status,
      String propertyIdentifier,
      String tenantIdentifier,
      Integer page,
      Integer size,
      String sort,
      String direction) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();

    // When filtering by property or tenant identifier, use the existing non-paginated methods
    // wrapped in PageResponse
    if (propertyIdentifier != null) {
      List<ContractResponse> results =
          contractService.getContractsByProperty(PropertyIdentifier.of(propertyIdentifier), principal);
      return PageResponse.of(results, 0, results.size(), results.size());
    }

    if (tenantIdentifier != null) {
      List<ContractResponse> results =
          contractService.getContractsByTenant(TenantIdentifier.of(tenantIdentifier), principal);
      return PageResponse.of(results, 0, results.size(), results.size());
    }

    PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
    return contractService.getContractsPaginated(principal, status, pageRequest);
  }

  @Override
  public ContractResponse getContract(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.getContract(ContractIdentifier.of(identifier), principal);
  }

  @Override
  public ContractResponse updateContract(
      String identifier, UpdateContractRequest updateContractRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.updateContract(ContractIdentifier.of(identifier), updateContractRequest, principal);
  }

  @Override
  public void deleteContract(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    contractService.deleteContract(ContractIdentifier.of(identifier), principal);
  }

  @Override
  public ContractResponse changeContractStatus(
      String identifier, ChangeContractStatusRequest changeContractStatusRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.changeContractStatus(ContractIdentifier.of(identifier), changeContractStatusRequest, principal);
  }

  @Override
  public ContractResponse reopenContract(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.reopenContract(ContractIdentifier.of(identifier), principal);
  }

  @Override
  public ContractResponse duplicateContract(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.duplicateContract(ContractIdentifier.of(identifier), principal);
  }

  @Override
  public CountryMetadataSchemaResponse getMetadataSchema(String countryCode) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
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

  @Override
  public ContractPartyResponse addParty(
      String identifier, AddContractPartyRequest addContractPartyRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractPartyService.addParty(ContractIdentifier.of(identifier), addContractPartyRequest, principal);
  }

  @Override
  public void removeParty(String identifier, String partyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    contractPartyService.removeParty(ContractIdentifier.of(identifier), ContractPartyIdentifier.of(partyIdentifier), principal);
  }

  @Override
  public ContractPartyResponse changePrimaryTenant(
      String identifier, ChangePrimaryTenantRequest changePrimaryTenantRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractPartyService.changePrimaryTenant(
        ContractIdentifier.of(identifier), changePrimaryTenantRequest, principal);
  }

  @Override
  @SuppressWarnings("NullAway")
  public DocumentResponse uploadContractDocument(
      String identifier, String title, String notes, UploadPhotoRequest uploadPhotoRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    // Generated interface mismodels multipart upload as JSON body
    return contractService.uploadDocument(ContractIdentifier.of(identifier), null, title, notes, principal);
  }

  @Override
  public List<DocumentResponse> getContractDocuments(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.getDocuments(ContractIdentifier.of(identifier), principal);
  }

  @Override
  public Map<String, String> getContractDocumentDownloadUrl(String documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    URL url = contractService.getDocumentDownloadUrl(DocumentIdentifier.of(documentIdentifier), principal);
    return Map.of("url", url.toString());
  }

  @Override
  public void deleteContractDocument(String documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    contractService.deleteDocument(DocumentIdentifier.of(documentIdentifier), principal);
  }

  @Override
  public List<RecentActivityResponse> getContractAuditLog(String identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.getAuditLog(ContractIdentifier.of(identifier), principal);
  }

  @Override
  public Map<String, Object> generatePayments(
      String identifier, GeneratePaymentsRequest generatePaymentsRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.generatePayments(ContractIdentifier.of(identifier), generatePaymentsRequest, principal);
  }
}
