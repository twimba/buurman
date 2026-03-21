package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.Optional;

import com.buurman.domain.RentComponentType;
import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record RentComponentResponse(
    Sid identifier,
    RentComponentType componentType,
    String componentTypeDisplayName,
    BigDecimal amount,
    String currency,
    Optional<String> description,
    int sortOrder) {}
