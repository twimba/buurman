package com.buurman.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.SignatureRequestIdentifier;
import com.buurman.dto.response.SignatureRequestResponse;
import com.buurman.generated.api.SignaturesApi;
import com.buurman.generated.model.CancelSignatureRequestRequest;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.esignature.SignatureService;

import lombok.RequiredArgsConstructor;

/** Sends contract documents for e-signature and reports on the resulting signature request. */
@RestController
@RequiredArgsConstructor
public class SignatureController implements SignaturesApi {

  private final SignatureService signatureService;

  @Override
  public SignatureRequestResponse createSignatureRequest(
      ContractIdentifier contractIdentifier, DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return signatureService.createSignatureRequest(
        contractIdentifier, documentIdentifier, principal);
  }

  @Override
  public List<SignatureRequestResponse> listSignatureRequests(
      ContractIdentifier contractIdentifier, DocumentIdentifier documentIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return signatureService.listSignatureRequests(
        contractIdentifier, documentIdentifier, principal);
  }

  @Override
  public SignatureRequestResponse cancelSignatureRequest(
      ContractIdentifier contractIdentifier,
      DocumentIdentifier documentIdentifier,
      SignatureRequestIdentifier signatureRequestIdentifier,
      Optional<CancelSignatureRequestRequest> cancelSignatureRequestRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    Optional<String> reason =
        cancelSignatureRequestRequest.map(CancelSignatureRequestRequest::getReason);
    return signatureService.cancelSignatureRequest(
        contractIdentifier, documentIdentifier, signatureRequestIdentifier, reason, principal);
  }

  @Override
  public SignatureRequestResponse getSignatureRequest(
      ContractIdentifier contractIdentifier,
      DocumentIdentifier documentIdentifier,
      SignatureRequestIdentifier signatureRequestIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return signatureService.getSignatureRequest(
        contractIdentifier, documentIdentifier, signatureRequestIdentifier, principal);
  }
}
