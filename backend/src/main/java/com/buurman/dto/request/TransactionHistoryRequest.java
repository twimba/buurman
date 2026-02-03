package com.buurman.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TransactionHistoryRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        TransactionType type,
        List<UUID> propertyIds,
        List<String> categories,
        int page,
        int size,
        String sort
) {
    public enum TransactionType {
        INCOME, EXPENSE, ALL
    }
}
