package com.buurman.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.ContractExtensionIdentifier;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.CreateContractExtensionRequest;
import com.buurman.dto.request.DeclineContractExtensionRequest;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.ContractExtensionResponse;
import com.buurman.dto.response.PageResponse;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.ContractExtensionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/contracts/{contractId}/extensions")
@RequiredArgsConstructor
public class ContractExtensionController {

  private final ContractExtensionService extensionService;

  @GetMapping
  public PageResponse<ContractExtensionResponse> listExtensions(
      @PathVariable ContractIdentifier contractId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String direction) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    PageRequest pageRequest = PageRequest.of(page, size, sort, direction);
    return extensionService.listExtensions(contractId, pageRequest, principal);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ContractExtensionResponse createExtension(
      @PathVariable ContractIdentifier contractId,
      @Valid @RequestBody CreateContractExtensionRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return extensionService.createExtension(contractId, request, principal);
  }

  @GetMapping("/{extensionId}")
  public ContractExtensionResponse getExtension(
      @PathVariable ContractIdentifier contractId,
      @PathVariable ContractExtensionIdentifier extensionId) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return extensionService.getExtension(contractId, extensionId, principal);
  }

  @PostMapping("/{extensionId}/activate")
  public ContractExtensionResponse activateExtension(
      @PathVariable ContractIdentifier contractId,
      @PathVariable ContractExtensionIdentifier extensionId) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return extensionService.activateExtension(contractId, extensionId, principal);
  }

  @PostMapping("/{extensionId}/confirm")
  public ContractExtensionResponse confirmExtension(
      @PathVariable ContractIdentifier contractId,
      @PathVariable ContractExtensionIdentifier extensionId) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return extensionService.confirmExtension(contractId, extensionId, principal);
  }

  @PostMapping("/{extensionId}/decline")
  public ContractExtensionResponse declineExtension(
      @PathVariable ContractIdentifier contractId,
      @PathVariable ContractExtensionIdentifier extensionId,
      @RequestBody(required = false) DeclineContractExtensionRequest request) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    if (request == null) {
      request = new DeclineContractExtensionRequest(java.util.Optional.empty());
    }
    return extensionService.declineExtension(contractId, extensionId, request, principal);
  }

  @DeleteMapping("/{extensionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelExtension(
      @PathVariable ContractIdentifier contractId,
      @PathVariable ContractExtensionIdentifier extensionId,
      @RequestParam(defaultValue = "false") boolean deleteDocuments) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    extensionService.cancelExtension(contractId, extensionId, deleteDocuments, principal);
  }
}
