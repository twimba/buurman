package com.buurman.dto.request;

import java.util.Objects;
import java.util.Optional;

import com.buurman.domain.Contract;

import jakarta.validation.constraints.NotNull;

public record ChangeContractStatusRequest(
    @NotNull(message = "Status is required") Contract.ContractStatus status,
    Optional<String> reason) {

  public ChangeContractStatusRequest {
    reason = Objects.requireNonNullElse(reason, Optional.empty());
  }
}
