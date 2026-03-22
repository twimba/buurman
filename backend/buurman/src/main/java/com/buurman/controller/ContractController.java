package com.buurman.controller;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.ContractPartyIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.AddContractPartyRequest;
import com.buurman.dto.request.ChangeContractStatusRequest;
import com.buurman.dto.request.ChangePrimaryContactRequest;
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
      Optional<String> status,
      Optional<String> propertyIdentifier,
      Optional<String> tenantIdentifier,
      Optional<Integer> page,
      Optional<Integer> size,
      Optional<String> sort,
      Optional<String> direction) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();

    // When filtering by property or tenant identifier, use the existing non-paginated methods
    // wrapped in PageResponse
    if (propertyIdentifier.isPresent()) {
      List<ContractResponse> results =
          contractService.getContractsByProperty(
              PropertyIdentifier.of(propertyIdentifier.get()), principal);
      return PageResponse.of(results, 0, results.size(), results.size());
    }

    if (tenantIdentifier.isPresent()) {
      List<ContractResponse> results =
          contractService.getContractsByContact(
              ContactIdentifier.of(tenantIdentifier.get()), principal);
      return PageResponse.of(results, 0, results.size(), results.size());
    }

    PageRequest pageRequest =
        PageRequest.of(
            page.orElse(null), size.orElse(null), sort.orElse(null), direction.orElse(null));
    return contractService.getContractsPaginated(principal, status.orElse(null), pageRequest);
  }

  @Override
  public ContractResponse getContract(ContractIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.getContract(identifier, principal);
  }

  @Override
  public ContractResponse updateContract(
      ContractIdentifier identifier, UpdateContractRequest updateContractRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.updateContract(identifier, updateContractRequest, principal);
  }

  @Override
  public void deleteContract(ContractIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    contractService.deleteContract(identifier, principal);
  }

  @Override
  public ContractResponse changeContractStatus(
      ContractIdentifier identifier, ChangeContractStatusRequest changeContractStatusRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.changeContractStatus(identifier, changeContractStatusRequest, principal);
  }

  @Override
  public ContractResponse reopenContract(ContractIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.reopenContract(identifier, principal);
  }

  @Override
  public ContractResponse duplicateContract(ContractIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.duplicateContract(identifier, principal);
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
      ContractIdentifier identifier, AddContractPartyRequest addContractPartyRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractPartyService.addParty(identifier, addContractPartyRequest, principal);
  }

  @Override
  public void removeParty(ContractIdentifier identifier, ContractPartyIdentifier partyIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    contractPartyService.removeParty(identifier, partyIdentifier, principal);
  }

  @Override
  public ContractPartyResponse changePrimaryContact(
      ContractIdentifier identifier, ChangePrimaryContactRequest changePrimaryContactRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractPartyService.changePrimaryContact(
        identifier, changePrimaryContactRequest, principal);
  }

  @Override
  public DocumentResponse uploadContractDocument(
      ContractIdentifier identifier,
      MultipartFile file,
      Optional<String> title,
      Optional<String> notes) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.uploadDocument(
        identifier, file, title.orElse(null), notes.orElse(null), principal);
  }

  @Override
  public List<DocumentResponse> getContractDocuments(ContractIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.getDocuments(identifier, principal);
  }

  @Override
  public Map<String, String> getContractDocumentDownloadUrl(DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    URL url = contractService.getDocumentDownloadUrl(documentIdentifier, principal);
    return Map.of("url", url.toString());
  }

  @Override
  public void deleteContractDocument(DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    contractService.deleteDocument(documentIdentifier, principal);
  }

  @Override
  public List<RecentActivityResponse> getContractAuditLog(ContractIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.getAuditLog(identifier, principal);
  }

  @Override
  public Map<String, Object> generatePayments(
      ContractIdentifier identifier, GeneratePaymentsRequest generatePaymentsRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return contractService.generatePayments(identifier, generatePaymentsRequest, principal);
  }
}
