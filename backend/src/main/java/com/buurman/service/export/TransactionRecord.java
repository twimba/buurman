package com.buurman.service.export;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

record TransactionRecord(
    String id,
    LocalDate date,
    String type,
    String description,
    String property,
    @Nullable String category,
    BigDecimal amount,
    String currency) {}
