package com.buurman.dto.request;

import static com.buurman.domain.ContractPartyRole.PRIMARY_TENANT;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Contract;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateContractRequest(
    @NotNull(message = "Property identifier is required") String propertyIdentifier,
    @NotNull(message = "At least one party is required") @Size(min = 1, message = "At least one party is required") @Valid List<ContractPartyRequest> parties,
    @NotNull(message = "Contract type is required") Contract.ContractType contractType,
    @NotNull(message = "Start date is required") LocalDate startDate,
    Optional<LocalDate> endDate,
    Optional<LocalDate> signedDate,
    @NotNull(message = "Rent amount is required") @Positive(message = "Rent amount must be positive") BigDecimal rentAmount,
    Optional<@PositiveOrZero(message = "Deposit amount must be zero or positive") BigDecimal>
        depositAmount,
    Optional<@PositiveOrZero(message = "Security deposit must be zero or positive") BigDecimal>
        securityDeposit,
    Optional<String> rentAmountCurrency,
    Optional<String> depositAmountCurrency,
    Optional<String> securityDepositCurrency,
    @NotNull(message = "Payment frequency is required") Contract.PaymentFrequency paymentFrequency,
    Optional<Integer> paymentDueDay,
    Optional<Boolean> autoRenewal,
    Optional<Integer> renewalNoticeDays,
    Optional<Integer> terminationNoticeDays,
    Optional<@PositiveOrZero(message = "Late fee percentage must be zero or positive") BigDecimal>
        lateFeePercentage,
    Optional<String> termsAndConditions,
    Optional<String> notes) {

  @AssertTrue(message = "End date must be after start date") public boolean isEndDateAfterStartDate() {
    return endDate.isEmpty() || !endDate.get().isBefore(startDate);
  }

  @AssertTrue(message = "Exactly one PRIMARY_TENANT party is required") public boolean isExactlyOnePrimaryTenant() {
    long count = parties.stream().filter(p -> p.role() == PRIMARY_TENANT).count();

    return count == 1;
  }
}
