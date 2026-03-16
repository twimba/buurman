package com.buurman.dto.request;

import java.util.Optional;

public record DeclineContractExtensionRequest(
    Optional<String> reason) {}
