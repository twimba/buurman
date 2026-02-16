package com.buurman.service.export;

import java.math.BigDecimal;
import java.time.LocalDate;

record TransactionRecord(
    String id,
    LocalDate date,
    String type,
    String description,
    String property,
    String category,
    BigDecimal amount,
    String currency) {}
