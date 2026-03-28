package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.Contract;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotNull;

@SkipTestCoverage
public record ChangeContractStatusRequest(
    @NotNull(message = "Status is required") Contract.ContractStatus status,
    Optional<String> reason) {}
