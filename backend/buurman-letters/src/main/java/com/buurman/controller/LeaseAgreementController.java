package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.UpdateContractLeaseClausesRequest;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.ResolvedLeaseClauseResponse;
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
  public List<ResolvedLeaseClauseResponse> getLeaseClauses(ContractIdentifier contractIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return leaseClauseService.getClauses(contractIdentifier, principal);
  }

  @Override
  public List<ResolvedLeaseClauseResponse> updateLeaseClauses(
      ContractIdentifier contractIdentifier,
      UpdateContractLeaseClausesRequest updateContractLeaseClausesRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return leaseClauseService.updateClauses(
        contractIdentifier, updateContractLeaseClausesRequest, principal);
  }

  @Override
  public DocumentResponse generateLeaseAgreement(ContractIdentifier contractIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return leaseAgreementGenerationService.generateAndPersist(contractIdentifier, principal);
  }
}
