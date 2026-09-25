package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.identifier.PropertyIdentifier;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateContractRequest(
    @NotNull(message = "Property identifier is required") PropertyIdentifier propertyIdentifier,
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
    Optional<Integer> terminationNoticeDays,
    Optional<Contract.RenewalMode> renewalMode,
    Optional<@Positive(message = "Renewal term must be positive") Integer> renewalTermMonths,
    Optional<@Positive(message = "Max renewals must be positive") Integer> maxRenewals,
    Optional<@PositiveOrZero(message = "Landlord notice days must be non-negative") Integer>
        landlordNoticeDays,
    Optional<@PositiveOrZero(message = "Tenant notice days must be non-negative") Integer>
        tenantNoticeDays,
    Optional<Boolean> requiresTenantConfirmation,
    Optional<ContractExtension.RentAdjustmentType> rentAdjustmentType,
    Optional<BigDecimal> rentAdjustmentValue,
    Optional<Contract.LandlordType> landlordType,
    Optional<String> regionCode,
    Optional<List<String>> documentLanguages,
    Optional<@PositiveOrZero(message = "Late fee percentage must be zero or positive") BigDecimal>
        lateFeePercentage,
    Optional<String> termsAndConditions,
    Optional<String> notes,
    @Nullable Map<String, Object> countryMetadata,
    Optional<List<@Valid RentComponentRequest>> rentComponents,
    Optional<Boolean> tenantRemindersEnabled,
    Optional<LocalDate> remindersPausedUntil,
    Optional<Boolean> lateFeeEnabled,
    Optional<@PositiveOrZero(message = "Grace days must be zero or positive") Integer>
        lateFeeGraceDays,
    Optional<
            @Min(value = 1, message = "Formal notice period must be at least 1 day") @Max(value = 365, message = "Formal notice period must be at most 365 days") Integer>
        formalNoticeDays) {

  // Bean Validation evaluates all constraints simultaneously, so @AssertTrue can run even when
  // @NotNull fails — null guards prevent NPE in that case.
  @AssertTrue(message = "End date must be after start date") @SuppressWarnings("ConstantConditions")
  public boolean isEndDateAfterStartDate() {
    return endDate.isEmpty() || startDate == null || !endDate.get().isBefore(startDate);
  }
}
