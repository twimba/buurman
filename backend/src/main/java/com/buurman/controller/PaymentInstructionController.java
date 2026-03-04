package com.buurman.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import com.buurman.domain.identifier.PaymentInstructionIdentifier;
import com.buurman.dto.request.CreatePaymentInstructionRequest;
import com.buurman.dto.request.UpdatePaymentInstructionRequest;
import com.buurman.dto.response.PaymentInstructionResponse;
import com.buurman.generated.api.PaymentInstructionsApi;
import com.buurman.security.SecurityUtils;
import com.buurman.security.UserPrincipal;
import com.buurman.service.PaymentInstructionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PaymentInstructionController implements PaymentInstructionsApi {

  private final PaymentInstructionService paymentInstructionService;

  @Override
  public List<PaymentInstructionResponse> getAll() {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentInstructionService.getAll(principal);
  }

  @Override
  public PaymentInstructionResponse getByIdentifier(PaymentInstructionIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentInstructionService.getByIdentifier(identifier, principal);
  }

  @Override
  public PaymentInstructionResponse createPaymentInstruction(
      @Valid CreatePaymentInstructionRequest createPaymentInstructionRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentInstructionService.create(createPaymentInstructionRequest, principal);
  }

  @Override
  public PaymentInstructionResponse updatePaymentInstruction(
      PaymentInstructionIdentifier identifier,
      @Valid UpdatePaymentInstructionRequest updatePaymentInstructionRequest) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    return paymentInstructionService.update(identifier, updatePaymentInstructionRequest, principal);
  }

  @Override
  public void deletePaymentInstruction(PaymentInstructionIdentifier identifier) {
    UserPrincipal principal = SecurityUtils.getCurrentPrincipal();
    paymentInstructionService.delete(identifier, principal);
  }
}
