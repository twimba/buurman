package com.buurman.service;

import static com.buurman.domain.Payment.PaymentStatus.CANCELLED;
import static com.buurman.domain.Payment.PaymentStatus.OVERDUE;
import static com.buurman.domain.Payment.PaymentStatus.PAID;
import static com.buurman.domain.Payment.PaymentStatus.PARTIALLY_PAID;
import static com.buurman.domain.Payment.PaymentStatus.PENDING;
import static com.buurman.util.SidGenerator.newPaymentId;
import static com.buurman.util.SidGenerator.newPaymentPlanId;
import static com.buurman.util.SidGenerator.newPaymentReceivalId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contract;
import com.buurman.domain.Payment;
import com.buurman.domain.Payment.PaymentType;
import com.buurman.domain.PaymentPlan;
import com.buurman.domain.PaymentPlan.Frequency;
import com.buurman.domain.PaymentPlan.PlanStatus;
import com.buurman.domain.PaymentReceival;
import com.buurman.domain.PaymentReceival.ReceivalType;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.PaymentPlanIdentifier;
import com.buurman.dto.request.CancelPaymentPlanRequest;
import com.buurman.dto.request.CreatePaymentPlanRequest;
import com.buurman.dto.response.PaymentPlanResponse;
import com.buurman.dto.response.PaymentSummary;
import com.buurman.exception.BusinessRuleException;
import com.buurman.mapper.PaymentMapper;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentPlanRepository;
import com.buurman.repository.PaymentReceivalRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Payment plans: settle a set of overdue payments with PLAN receivals and replace them with equal
 * INSTALMENT payments on a schedule. Tenant reminders on the contract are paused until the last
 * instalment is due so the ladder does not chase a debt that is being paid off as agreed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentPlanService {

  private final PaymentPlanRepository planRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentReceivalRepository receivalRepository;
  private final ContractRepository contractRepository;
  private final PaymentMapper paymentMapper;
  private final AuditService auditService;
  private final MetricsService metricsService;
  private final Clock clock;

  @Transactional(readOnly = true)
  public List<PaymentPlanResponse> getPlans(
      ContractIdentifier contractIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    return planRepository.findByContractIdAndTeamId(contract.getId(), teamId).stream()
        .map(plan -> toResponse(plan, contract, teamId))
        .toList();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentPlanResponse createPlan(
      ContractIdentifier contractIdentifier,
      CreatePaymentPlanRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    LocalDate today = LocalDate.now(clock);

    List<Sid> requested =
        request.paymentIdentifiers().stream().map(Sid.class::cast).distinct().toList();
    List<Payment> covered = paymentRepository.findByIdentifiersAndTeamId(requested, teamId);
    if (covered.size() != requested.size()) {
      throw new BusinessRuleException("One or more payments were not found");
    }
    String currency = contract.getRentAmount().currency();
    BigDecimal total = BigDecimal.ZERO;
    List<BigDecimal> balances = new ArrayList<>();
    for (Payment p : covered) {
      if (!p.getContractId().equals(contract.getId())) {
        throw new BusinessRuleException("All payments must belong to this contract");
      }
      if (p.getStatus() == PAID || p.getStatus() == CANCELLED) {
        throw new BusinessRuleException(
            "Payment " + p.getIdentifier().orElseThrow() + " is not open");
      }
      if (p.getPaymentType() == PaymentType.INSTALMENT) {
        throw new BusinessRuleException("Instalments cannot be covered by another plan");
      }
      if (!p.getAmount().currency().equals(currency)) {
        throw new BusinessRuleException("All payments must be in the contract currency");
      }
      BigDecimal balance =
          p.getAmount()
              .value()
              .subtract(receivalRepository.sumByPaymentIdAndTeamId(p.getId(), teamId, currency));
      if (balance.signum() <= 0) {
        throw new BusinessRuleException(
            "Payment " + p.getIdentifier().orElseThrow() + " has no balance");
      }
      balances.add(balance);
      total = total.add(balance);
    }

    Frequency frequency = request.frequency().orElse(Frequency.MONTHLY);
    PaymentPlan plan =
        PaymentPlan.builder()
            .identifier(Optional.of(newPaymentPlanId()))
            .teamId(teamId)
            .contractId(contract.getId())
            .contactId(covered.getFirst().getContactId())
            .totalAmount(MoneyAmount.of(total, currency))
            .instalmentCount(request.instalmentCount())
            .startDate(request.startDate())
            .frequency(frequency)
            .notes(request.notes().filter(n -> !n.isBlank()))
            .createdBy(principal.getUserId())
            .updatedBy(principal.getUserId())
            .build();
    PaymentPlan saved = planRepository.save(plan);

    // Settle the covered payments into the plan
    for (int i = 0; i < covered.size(); i++) {
      Payment p = covered.get(i);
      PaymentReceival settle = new PaymentReceival();
      settle.setIdentifier(Optional.of(newPaymentReceivalId()));
      settle.setTeamId(teamId);
      settle.setPaymentId(p.getId());
      settle.setAmount(MoneyAmount.of(balances.get(i), currency));
      settle.setReceivalDate(today);
      settle.setNotes(
          Optional.of("Moved into payment plan " + saved.getIdentifier().orElseThrow()));
      settle.setReceivalType(ReceivalType.PLAN);
      settle.setPaymentPlanId(Optional.of(saved.getId()));
      settle.setCreatedBy(principal.getUserId());
      settle.setUpdatedBy(principal.getUserId());
      settle.setCreatedAt(clock.instant());
      settle.setUpdatedAt(clock.instant());
      receivalRepository.save(settle);
      p.setStatus(PAID);
      p.setPaymentDate(Optional.of(today));
      p.setUpdatedBy(principal.getUserId());
      p.setUpdatedAt(clock.instant());
      paymentRepository.save(p);
    }

    // Create the instalments
    List<BigDecimal> split = splitAmount(total, request.instalmentCount(), currency);
    LocalDate lastDue = request.startDate();
    for (int i = 0; i < split.size(); i++) {
      LocalDate due = dueDate(request.startDate(), frequency, i);
      lastDue = due;
      Payment instalment = new Payment();
      instalment.setIdentifier(Optional.of(newPaymentId()));
      instalment.setTeamId(teamId);
      instalment.setContractId(contract.getId());
      instalment.setContactId(saved.getContactId());
      instalment.setAmount(MoneyAmount.of(split.get(i), currency));
      instalment.setDueDate(due);
      instalment.setStatus(due.isBefore(today) ? OVERDUE : PENDING);
      instalment.setPaymentType(PaymentType.INSTALMENT);
      instalment.setPaymentPlanId(Optional.of(saved.getId()));
      instalment.setAutoGenerated(true);
      instalment.setNotes(
          Optional.of(
              "Instalment "
                  + (i + 1)
                  + "/"
                  + split.size()
                  + " of payment plan "
                  + saved.getIdentifier().orElseThrow()));
      instalment.setCreatedAt(clock.instant());
      instalment.setUpdatedAt(clock.instant());
      instalment.setCreatedBy(principal.getUserId());
      instalment.setUpdatedBy(principal.getUserId());
      paymentRepository.save(instalment);
    }

    if (request.pauseReminders().orElse(true)) {
      contract.setRemindersPausedUntil(Optional.of(lastDue));
      contract.setUpdatedBy(principal.getUserId());
      contract.setUpdatedAt(clock.instant());
      contractRepository.save(contract);
    }

    auditService.logCreate(teamId, "PAYMENT_PLAN", saved.getId(), principal.getUserId(), saved);
    metricsService.incrementCounter("payment.plan.created.total");
    log.info(
        "Created payment plan {} for contract {}: {} {} in {} instalments from {}",
        saved.getIdentifier().orElseThrow(),
        contractIdentifier,
        total,
        currency,
        request.instalmentCount(),
        request.startDate());
    return toResponse(saved, contract, teamId);
  }

  /**
   * Cancels the plan: open instalments are cancelled and the unpaid remainder becomes one payment
   * due today.
   */
  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentPlanResponse cancelPlan(
      ContractIdentifier contractIdentifier,
      PaymentPlanIdentifier planIdentifier,
      CancelPaymentPlanRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    PaymentPlan plan = planRepository.getByIdentifierAndTeamId(planIdentifier, teamId);
    if (!plan.getContractId().equals(contract.getId())) {
      throw new BusinessRuleException("Plan does not belong to this contract");
    }
    if (plan.getStatus() != PlanStatus.ACTIVE) {
      throw new BusinessRuleException("Only active plans can be cancelled");
    }
    LocalDate today = LocalDate.now(clock);
    String currency = plan.getTotalAmount().currency();
    BigDecimal remainder = BigDecimal.ZERO;
    for (Payment instalment : paymentRepository.findByPaymentPlanId(plan.getId(), teamId)) {
      if (instalment.getStatus() == PAID || instalment.getStatus() == CANCELLED) {
        continue;
      }
      BigDecimal balance =
          instalment
              .getAmount()
              .value()
              .subtract(
                  receivalRepository.sumByPaymentIdAndTeamId(instalment.getId(), teamId, currency));
      remainder = remainder.add(balance.max(BigDecimal.ZERO));
      if (balance.signum() > 0 && instalment.getStatus() == PARTIALLY_PAID) {
        // keep what was paid, close the rest with a PLAN receival so the instalment reconciles
        PaymentReceival close = new PaymentReceival();
        close.setIdentifier(Optional.of(newPaymentReceivalId()));
        close.setTeamId(teamId);
        close.setPaymentId(instalment.getId());
        close.setAmount(MoneyAmount.of(balance, currency));
        close.setReceivalDate(today);
        close.setNotes(Optional.of("Plan cancelled; balance moved to a new payment"));
        close.setReceivalType(ReceivalType.PLAN);
        close.setPaymentPlanId(Optional.of(plan.getId()));
        close.setCreatedBy(principal.getUserId());
        close.setUpdatedBy(principal.getUserId());
        close.setCreatedAt(clock.instant());
        close.setUpdatedAt(clock.instant());
        receivalRepository.save(close);
        instalment.setStatus(PAID);
      } else {
        instalment.setStatus(CANCELLED);
        instalment.setCancelReason(Optional.of("Payment plan cancelled: " + request.reason()));
      }
      instalment.setUpdatedBy(principal.getUserId());
      instalment.setUpdatedAt(clock.instant());
      paymentRepository.save(instalment);
    }
    if (remainder.signum() > 0) {
      Payment reopened = new Payment();
      reopened.setIdentifier(Optional.of(newPaymentId()));
      reopened.setTeamId(teamId);
      reopened.setContractId(contract.getId());
      reopened.setContactId(plan.getContactId());
      reopened.setAmount(MoneyAmount.of(remainder, currency));
      reopened.setDueDate(today);
      reopened.setStatus(PENDING);
      reopened.setPaymentType(PaymentType.RENT);
      reopened.setNotes(
          Optional.of(
              "Remaining balance of cancelled payment plan " + plan.getIdentifier().orElseThrow()));
      reopened.setCreatedAt(clock.instant());
      reopened.setUpdatedAt(clock.instant());
      reopened.setCreatedBy(principal.getUserId());
      reopened.setUpdatedBy(principal.getUserId());
      paymentRepository.save(reopened);
    }
    PaymentPlan before = plan.toBuilder().build();
    plan.setStatus(PlanStatus.CANCELLED);
    plan.setCancelReason(Optional.of(request.reason()));
    plan.setUpdatedBy(principal.getUserId());
    plan.setUpdatedAt(clock.instant());
    PaymentPlan saved = planRepository.save(plan);
    contract.setRemindersPausedUntil(Optional.empty());
    contract.setUpdatedBy(principal.getUserId());
    contract.setUpdatedAt(clock.instant());
    contractRepository.save(contract);
    auditService.logUpdate(
        teamId,
        "PAYMENT_PLAN",
        saved.getId(),
        principal.getUserId(),
        before,
        saved,
        Map.of("cancelReason", request.reason(), "reopenedBalance", remainder.toPlainString()));
    metricsService.incrementCounter("payment.plan.cancelled.total");
    return toResponse(saved, contract, teamId);
  }

  /** Equal instalments rounded to the currency's minor unit; the last one absorbs the rounding. */
  static List<BigDecimal> splitAmount(BigDecimal total, int count, String currency) {
    int digits = CurrencyUtils.getFractionalDigits(currency);
    BigDecimal each = total.divide(BigDecimal.valueOf(count), digits, RoundingMode.DOWN);
    List<BigDecimal> parts = new ArrayList<>();
    BigDecimal allocated = BigDecimal.ZERO;
    for (int i = 0; i < count - 1; i++) {
      parts.add(each);
      allocated = allocated.add(each);
    }
    parts.add(total.subtract(allocated));
    return parts;
  }

  static LocalDate dueDate(LocalDate start, Frequency frequency, int index) {
    return switch (frequency) {
      case WEEKLY -> start.plusWeeks(index);
      case BIWEEKLY -> start.plusWeeks(2L * index);
      case MONTHLY -> start.plusMonths(index);
    };
  }

  private PaymentPlanResponse toResponse(PaymentPlan plan, Contract contract, UUID teamId) {
    List<Payment> instalments = paymentRepository.findByPaymentPlanId(plan.getId(), teamId);
    String currency = plan.getTotalAmount().currency();
    BigDecimal paid = BigDecimal.ZERO;
    for (Payment p : instalments) {
      if (p.getStatus() == CANCELLED) {
        continue;
      }
      paid = paid.add(receivalRepository.sumByPaymentIdAndTeamId(p.getId(), teamId, currency));
    }
    List<Sid> covered =
        receivalRepository.findPaymentIdsByPlanId(plan.getId(), teamId).stream()
            .map(id -> paymentRepository.findByIdAndTeamId(id, teamId))
            .flatMap(Optional::stream)
            .flatMap(p -> p.getIdentifier().stream())
            .toList();
    List<PaymentSummary> summaries = instalments.stream().map(paymentMapper::toSummary).toList();
    return new PaymentPlanResponse(
        plan.getIdentifier().orElseThrow(),
        contract.getIdentifier().orElseThrow(),
        plan.getTotalAmount().value(),
        currency,
        plan.getInstalmentCount(),
        plan.getStartDate(),
        plan.getFrequency(),
        plan.getStatus(),
        plan.getNotes(),
        plan.getCancelReason(),
        paid,
        plan.getTotalAmount().value().subtract(paid).max(BigDecimal.ZERO),
        summaries,
        covered,
        plan.getCreatedAt());
  }
}
