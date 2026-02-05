package com.buurman.dto.request;

import com.buurman.domain.Contract;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateContractRequest(
        @NotNull(message = "Property ID is required")
        UUID propertyId,

        @NotNull(message = "Tenant ID is required")
        UUID tenantId,

        @NotNull(message = "Contract type is required")
        Contract.ContractType contractType,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate,

        LocalDate signedDate,

        @NotNull(message = "Rent amount is required")
        @Positive(message = "Rent amount must be positive")
        BigDecimal rentAmount,

        BigDecimal depositAmount,

        BigDecimal securityDeposit,

        String currency,

        @NotNull(message = "Payment frequency is required")
        Contract.PaymentFrequency paymentFrequency,

        Integer paymentDueDay,

        Boolean autoRenewal,

        Integer renewalNoticeDays,

        Integer terminationNoticeDays,

        BigDecimal lateFeePercentage,

        String termsAndConditions,

        String notes
) {
    @AssertTrue(message = "End date must be after start date")
    public boolean isEndDateAfterStartDate() {
        return endDate == null || startDate == null || !endDate.isBefore(startDate);
    }
}
