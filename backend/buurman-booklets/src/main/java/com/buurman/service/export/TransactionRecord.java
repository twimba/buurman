package com.buurman.service.export;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public record TransactionRecord(
    String id,
    LocalDate date,
    String type,
    String description,
    String property,
    Optional<String> category,
    BigDecimal amount,
    String currency) {}
