package com.buurman.service;

import static com.buurman.domain.Payment.PaymentStatus.CANCELLED;
import static com.buurman.domain.Payment.PaymentStatus.PAID;
import static com.buurman.domain.Payment.PaymentStatus.PENDING;
import static com.buurman.util.SidGenerator.newPaymentId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.buurman.domain.Contract;
import com.buurman.domain.LateFeePolicy;
import com.buurman.domain.Payment;
import com.buurman.domain.Payment.PaymentType;
import com.buurman.domain.RentRegulationCountry;
import com.buurman.domain.identifier.PaymentIdentifier;
import com.buurman.dto.request.WaiveLateFeeRequest;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentReceivalRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.RentRegulationRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.Constants;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Late fees: a separate LATE_FEE payment charged once on a rent payment that is still open after
 * the contract's grace period. Opt-in per contract ({@code lateFeeEnabled} + {@code
 * lateFeePercentage}); a charged fee can be waived with a reason.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LateFeeService {

  private final PaymentRepository paymentRepository;
  private final ContractRepository contractRepository;
  private final RentRegulationRepository rentRegulationRepository;
  private final PaymentReceivalRepository receivalRepository;
  private final AuditService auditService;
  private final MetricsService metricsService;
  private final Clock clock;
  private final PlatformTransactionManager transactionManager;

  /** Incident kill switch: {@code scheduling.late-fees.enabled=false} skips the run. */
  @Value("${scheduling.late-fees.enabled:true}")
  private boolean enabled = true;

  /**
   * Charges one late fee per eligible overdue rent payment. Idempotent across runs: the candidate
   * query excludes rents that already carry a fee. Each charge commits on its own so one bad row
   * cannot roll back a whole day's fees.
   */
  public int runDailyLateFees() {
    if (!enabled) {
      log.warn("Late-fee run is disabled by configuration; skipping");
      return 0;
    }
    Instant start = clock.instant();
    LocalDate today = LocalDate.now(clock);
    Map<String, Optional<RentRegulationCountry>> regulationCache = new HashMap<>();
    Set<String> unknownPolicyWarned = new HashSet<>();
    TransactionTemplate tx = new TransactionTemplate(transactionManager);
    List<Payment> candidates = paymentRepository.findLateFeeCandidates(today);
    Map<UUID, List<Payment>> byTeam =
        candidates.stream().collect(Collectors.groupingBy(Payment::getTeamId));
    int charged = 0;
    for (Map.Entry<UUID, List<Payment>> team : byTeam.entrySet()) {
      UUID teamId = team.getKey();
      List<Payment> rents = team.getValue();
      Map<UUID, Contract> contracts =
          contractRepository
              .findByIdsAndTeamId(rents.stream().map(Payment::getContractId).toList(), teamId)
              .stream()
              .collect(Collectors.toMap(Contract::getId, Function.identity()));
      Map<UUID, BigDecimal> received = new HashMap<>();
      rents.stream()
          .collect(Collectors.groupingBy(r -> r.getAmount().currency()))
          .forEach(
              (currency, group) ->
                  received.putAll(
                      receivalRepository.sumByPaymentIdsAndTeamId(
                          group.stream().map(Payment::getId).toList(), teamId, currency)));
      for (Payment rent : rents) {
        Contract contract = contracts.get(rent.getContractId());
        if (contract == null) {
          continue;
        }
        Optional<RentRegulationCountry> regulation =
            contract
                .getCountryCode()
                .flatMap(
                    code ->
                        regulationCache.computeIfAbsent(
                            code, rentRegulationRepository::findCountryByCode));
        if (regulation.map(r -> r.getLateFeePolicy() == LateFeePolicy.UNKNOWN).orElse(false)) {
          // Catalogue row exists but its late-fee regime was never loaded: never charge blind.
          metricsService.incrementCounter(
              "payment.late_fee.skipped.total", "reason", "unknown_policy");
          if (unknownPolicyWarned.add(contract.getCountryCode().orElse("?"))) {
            log.warn(
                "Late-fee policy for {} is UNKNOWN; reload the rent-regulation catalogue. Skipping"
                    + " fees for this country.",
                contract.getCountryCode().orElse("?"));
          }
          continue;
        }
        BigDecimal outstanding =
            rent.getAmount().value().subtract(received.getOrDefault(rent.getId(), BigDecimal.ZERO));
        Optional<BigDecimal> fee =
            computeFee(outstanding, rent.getAmount().currency(), contract, regulation);
        if (fee.isEmpty()) {
          metricsService.incrementCounter(
              "payment.late_fee.skipped.total", "reason", "not_applicable");
          continue;
        }
        try {
          tx.executeWithoutResult(status -> chargeLateFee(rent, contract, fee.get(), today));
          charged++;
        } catch (Exception e) {
          metricsService.incrementCounter("payment.late_fee.failed.total");
          log.error(
              "Failed to charge late fee on payment {} (team {}): {}",
              rent.getIdentifier().map(Object::toString).orElse("?"),
              teamId,
              e.getMessage(),
              e);
        }
      }
    }
    metricsService.recordTimer(
        "payment.late_fee.run.duration", Duration.between(start, clock.instant()));
    log.info(
        "Late fee run completed — {} candidate(s), {} fee(s) charged", candidates.size(), charged);
    return charged;
  }

  /** Fee on the full face value (nothing received yet). */
  static Optional<BigDecimal> computeFee(Payment rent, Contract contract) {
    return computeFee(
        rent.getAmount().value(), rent.getAmount().currency(), contract, Optional.empty());
  }

  static Optional<BigDecimal> computeFee(
      Payment rent, Contract contract, Optional<RentRegulationCountry> regulation) {
    return computeFee(rent.getAmount().value(), rent.getAmount().currency(), contract, regulation);
  }

  /**
   * Percentage of the amount still due, rounded to the currency's minor unit. The jurisdiction's
   * late-fee policy is applied on top of the contract: a forbidden or interest-only regime yields
   * no fee, a capped one clamps the percentage (caps are expressed on the amount due, which is why
   * a partially paid rent is charged on its balance).
   */
  static Optional<BigDecimal> computeFee(
      BigDecimal amountDue,
      String currency,
      Contract contract,
      Optional<RentRegulationCountry> regulation) {
    if (amountDue.signum() <= 0) {
      return Optional.empty();
    }
    if (!Boolean.TRUE.equals(contract.getLateFeeEnabled())) {
      return Optional.empty();
    }
    Optional<BigDecimal> cap = effectiveCap(regulation);
    if (regulation
        .map(RentRegulationCountry::getLateFeePolicy)
        .filter(p -> p == LateFeePolicy.FORBIDDEN || p == LateFeePolicy.INTEREST_ONLY)
        .isPresent()) {
      return Optional.empty();
    }
    return contract
        .getLateFeePercentage()
        .filter(pct -> pct.signum() > 0)
        .map(pct -> cap.map(pct::min).orElse(pct))
        .map(
            pct ->
                amountDue
                    .multiply(pct)
                    .divide(
                        BigDecimal.valueOf(100),
                        CurrencyUtils.getFractionalDigits(currency),
                        RoundingMode.HALF_UP))
        .filter(fee -> fee.signum() > 0);
  }

  /** Statutory maximum percentage when the policy is CAPPED and a value is known. */
  static Optional<BigDecimal> effectiveCap(Optional<RentRegulationCountry> regulation) {
    return regulation
        .filter(r -> r.getLateFeePolicy() == LateFeePolicy.CAPPED)
        .flatMap(RentRegulationCountry::getLateFeeMaxPercentage);
  }

  /**
   * Validates a contract's late-fee settings against the jurisdiction. Throws when the regime
   * forbids flat fees or the percentage exceeds the statutory cap; silent when unknown.
   */
  public static void validateAgainstRegulation(
      Contract contract, Optional<RentRegulationCountry> regulation) {
    if (!Boolean.TRUE.equals(contract.getLateFeeEnabled())) {
      return;
    }
    if (regulation.isEmpty()) {
      return;
    }
    RentRegulationCountry country = regulation.get();
    switch (country.getLateFeePolicy()) {
      case FORBIDDEN ->
          throw new BusinessRuleException(
              "Late fees on residential rent are not permitted in "
                  + country.getCountryName()
                  + country.getLateFeeNotes().map(n -> " (" + n + ")").orElse(""));
      case INTEREST_ONLY ->
          throw new BusinessRuleException(
              "Only statutory default interest may be charged on overdue rent in "
                  + country.getCountryName()
                  + "; flat late fees cannot be enabled"
                  + country.getLateFeeNotes().map(n -> " (" + n + ")").orElse(""));
      case CAPPED -> {
        Optional<BigDecimal> max = country.getLateFeeMaxPercentage();
        Optional<BigDecimal> pct = contract.getLateFeePercentage();
        if (max.isPresent() && pct.isPresent() && pct.get().compareTo(max.get()) > 0) {
          throw new BusinessRuleException(
              "Late fee of "
                  + pct.get().stripTrailingZeros().toPlainString()
                  + "% exceeds the maximum of "
                  + max.get().stripTrailingZeros().toPlainString()
                  + "% permitted in "
                  + country.getCountryName());
        }
      }
      default -> {
        // ALLOWED / UNKNOWN: nothing to enforce
      }
    }
  }

  private void chargeLateFee(Payment rent, Contract contract, BigDecimal fee, LocalDate today) {
    Payment charge = new Payment();
    charge.setIdentifier(Optional.of(newPaymentId()));
    charge.setTeamId(rent.getTeamId());
    charge.setContractId(rent.getContractId());
    charge.setContactId(rent.getContactId());
    charge.setAmount(MoneyAmount.of(fee, rent.getAmount().currency()));
    charge.setDueDate(today);
    charge.setStatus(PENDING);
    charge.setPaymentType(PaymentType.LATE_FEE);
    charge.setParentPaymentId(Optional.of(rent.getId()));
    charge.setAutoGenerated(true);
    charge.setNotes(
        Optional.of(
            "Late fee ("
                + contract
                    .getLateFeePercentage()
                    .orElse(BigDecimal.ZERO)
                    .stripTrailingZeros()
                    .toPlainString()
                + "%) on payment "
                + rent.getIdentifier().map(Object::toString).orElse("")
                + " due "
                + rent.getDueDate()));
    charge.setCreatedAt(clock.instant());
    charge.setUpdatedAt(clock.instant());
    charge.setCreatedBy(Constants.SYSTEM_USER_ID);
    charge.setUpdatedBy(Constants.SYSTEM_USER_ID);
    Payment saved = paymentRepository.save(charge);

    auditService.logCreate(
        rent.getTeamId(), "PAYMENT", saved.getId(), Constants.SYSTEM_USER_ID, saved);
    metricsService.incrementCounter("payment.late_fee.charged.total");
    metricsService.incrementCounterBy(
        "payment.late_fee.amount.total",
        fee.doubleValue(),
        "currency",
        rent.getAmount().currency());
    log.info(
        "Charged late fee {} of {} {} on payment {}",
        saved.getIdentifier().orElseThrow(),
        fee,
        rent.getAmount().currency(),
        rent.getIdentifier().map(Object::toString).orElse("?"));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public Payment waiveLateFee(
      PaymentIdentifier identifier, WaiveLateFeeRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Payment fee = paymentRepository.getByIdentifierAndTeamId(identifier, teamId);
    if (fee.getPaymentType() != PaymentType.LATE_FEE) {
      throw new BusinessRuleException("Only late fees can be waived");
    }
    if (fee.getStatus() == PAID) {
      throw new BusinessRuleException("A paid late fee cannot be waived");
    }
    if (fee.getStatus() == CANCELLED) {
      throw new BusinessRuleException("Late fee is already waived or cancelled");
    }
    Payment before = fee.toBuilder().build();
    fee.setStatus(CANCELLED);
    fee.setWaivedAt(Optional.of(clock.instant()));
    fee.setWaivedBy(Optional.of(principal.getUserId()));
    fee.setWaiveReason(Optional.of(request.reason()));
    fee.setCancelReason(Optional.of("Waived: " + request.reason()));
    fee.setUpdatedBy(principal.getUserId());
    fee.setUpdatedAt(clock.instant());
    Payment saved = paymentRepository.save(fee);
    auditService.logUpdate(
        teamId,
        "PAYMENT",
        saved.getId(),
        principal.getUserId(),
        before,
        saved,
        java.util.Map.of("status", "CANCELLED", "waiveReason", request.reason()));
    metricsService.incrementCounter("payment.late_fee.waived.total");
    return saved;
  }
}
