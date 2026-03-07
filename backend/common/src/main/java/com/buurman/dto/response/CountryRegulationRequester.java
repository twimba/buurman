package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

public record CountryRegulationRequester(
    String userIdentifier,
    String userName,
    String teamIdentifier,
    String teamName,
    Optional<String> notes,
    Instant requestedAt) {}
