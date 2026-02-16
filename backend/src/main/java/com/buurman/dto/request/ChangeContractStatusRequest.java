package com.buurman.dto.request;

import com.buurman.domain.Contract;

import jakarta.validation.constraints.NotNull;

public record ChangeContractStatusRequest(
    @NotNull(message = "Status is required") Contract.ContractStatus status, String reason) {}
