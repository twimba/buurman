package com.buurman.controller;

import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.document.DocumentLocale;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.UpdateContractLeaseClausesRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.LeaseClausesResponse;
import com.buurman.generated.api.LeaseAgreementApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.LeaseClauseService;
import com.buurman.service.letters.LeaseAgreementGenerationService;

import lombok.RequiredArgsConstructor;

/** Contract-facing clause toggle and full lease agreement generate-and-persist. */
@RestController
@RequiredArgsConstructor
public class LeaseAgreementController implements LeaseAgreementApi {

  private final LeaseClauseService leaseClauseService;
  private final LeaseAgreementGenerationService leaseAgreementGenerationService;

  @Override
  public LeaseClausesResponse getLeaseClauses(ContractIdentifier contractIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return leaseClauseService.getClauses(contractIdentifier, principal);
  }

  @Override
  public LeaseClausesResponse updateLeaseClauses(
      ContractIdentifier contractIdentifier,
      UpdateContractLeaseClausesRequest updateContractLeaseClausesRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return leaseClauseService.updateClauses(
        contractIdentifier, updateContractLeaseClausesRequest, principal);
  }

  @Override
  public DocumentResponse generateLeaseAgreement(
      ContractIdentifier contractIdentifier, Optional<String> lang) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    // Strict like the other letters: an unsupported code is a 400, never a silent fallback, since
    // the generated agreement is persisted as a contract document.
    Optional<String> language = lang.map(code -> DocumentLocale.resolve(code).getLanguage());
    return leaseAgreementGenerationService.generateAndPersist(
        contractIdentifier, language, principal);
  }
}
