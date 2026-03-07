package com.buurman.controller;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.ContractPaymentInstructionIdentifier;
import com.buurman.dto.request.CreateContractPaymentInstructionRequest;
import com.buurman.dto.request.UpdateContractPaymentInstructionRequest;
import com.buurman.dto.response.ContractPaymentInstructionResponse;
import com.buurman.generated.api.ContractPaymentInstructionsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ContractPaymentInstructionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ContractPaymentInstructionController implements ContractPaymentInstructionsApi {

  private final ContractPaymentInstructionService service;

  @Override
  public List<ContractPaymentInstructionResponse> getHistory(
      ContractIdentifier contractIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return service.getHistory(contractIdentifier, principal);
  }

  @Override
  public @Nullable ContractPaymentInstructionResponse getCurrent(
      ContractIdentifier contractIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return service.getCurrent(contractIdentifier, principal).orElse(null);
  }

  @Override
  public ContractPaymentInstructionResponse createContractPaymentInstruction(
      ContractIdentifier contractIdentifier,
      CreateContractPaymentInstructionRequest createContractPaymentInstructionRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return service.create(contractIdentifier, createContractPaymentInstructionRequest, principal);
  }

  @Override
  public ContractPaymentInstructionResponse updateContractPaymentInstruction(
      ContractIdentifier contractIdentifier,
      ContractPaymentInstructionIdentifier instructionIdentifier,
      UpdateContractPaymentInstructionRequest updateContractPaymentInstructionRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return service.update(
        contractIdentifier,
        instructionIdentifier,
        updateContractPaymentInstructionRequest,
        principal);
  }

  @Override
  public void deleteContractPaymentInstruction(
      ContractIdentifier contractIdentifier,
      ContractPaymentInstructionIdentifier instructionIdentifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    service.delete(contractIdentifier, instructionIdentifier, principal);
  }
}
