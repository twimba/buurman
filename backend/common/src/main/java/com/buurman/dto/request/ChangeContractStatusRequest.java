package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.Contract;

import jakarta.validation.constraints.NotNull;
import com.buurman.util.Generated;

@Generated
public record ChangeContractStatusRequest(
    @NotNull(message = "Status is required") Contract.ContractStatus status,
    Optional<String> reason) {}
