package com.buurman.service;

import static com.buurman.util.SidGenerator.newDepositDeductionId;
import static com.buurman.util.SidGenerator.newDepositId;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contract;
import com.buurman.domain.Deposit;
import com.buurman.domain.Deposit.DepositStatus;
import com.buurman.domain.DepositDeduction;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.DepositDeductionIdentifier;
import com.buurman.dto.request.CreateDepositDeductionRequest;
import com.buurman.dto.request.ForfeitDepositRequest;
import com.buurman.dto.request.ReturnDepositRequest;
import com.buurman.dto.request.UpsertDepositRequest;
import com.buurman.dto.response.DepositResponse;
import com.buurman.dto.response.DepositResponse.DepositDeductionResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DepositRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Deposit lifecycle per contract: expected → held → (deductions) → returned / forfeited. The
 * contract's own depositAmount field stays the agreed figure; the deposit record tracks what
 * actually happened to the money.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DepositService {

  private final DepositRepository depositRepository;
  private final ContractRepository contractRepository;
  private final ContractPartyService contractPartyService;
  private final AuditService auditService;
  private final MetricsService metricsService;
  private final Clock clock;

  @Transactional(readOnly = true)
  public List<DepositResponse> getDeposits(
      ContractIdentifier contractIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    return depositRepository
        .findByContractIdAndTeamId(contract.getId(), teamId)
        .map(d -> List.of(toResponse(d, contract)))
        .orElse(List.of());
  }

  /** Creates the deposit record or updates its agreed amount, dates and notes. */
  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public DepositResponse upsert(
      ContractIdentifier contractIdentifier,
      UpsertDepositRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    String currency = contract.getRentAmount().currency();
    Optional<Deposit> existing =
        depositRepository.findByContractIdAndTeamId(contract.getId(), teamId);

    Deposit deposit =
        existing.orElseGet(
            () ->
                Deposit.builder()
                    .identifier(Optional.of(newDepositId()))
                    .teamId(teamId)
                    .contractId(contract.getId())
                    .contactId(
                        contractPartyService
                            .findPrimaryContactForContract(contract.getId(), teamId)
                            .map(c -> c.getId()))
                    .createdBy(principal.getUserId())
                    .build());
    if (deposit.getStatus() == DepositStatus.RETURNED
        || deposit.getStatus() == DepositStatus.FORFEITED) {
      throw new BusinessRuleException("A returned or forfeited deposit can no longer be edited");
    }
    Deposit before = existing.map(d -> d.toBuilder().build()).orElse(null);
    deposit.setAmount(MoneyAmount.of(request.amount(), currency));
    deposit.setReceivedDate(request.receivedDate());
    deposit.setHeldWhere(request.heldWhere().filter(h -> !h.isBlank()));
    deposit.setReturnDueDate(request.returnDueDate());
    deposit.setNotes(request.notes().filter(n -> !n.isBlank()));
    if (deposit.getStatus() == DepositStatus.EXPECTED && request.receivedDate().isPresent()) {
      deposit.setStatus(DepositStatus.HELD);
    }
    deposit.setUpdatedBy(principal.getUserId());
    deposit.setUpdatedAt(clock.instant());
    Deposit saved = depositRepository.save(deposit);
    if (before == null) {
      auditService.logCreate(teamId, "DEPOSIT", saved.getId(), principal.getUserId(), saved);
      metricsService.incrementCounter("deposit.created.total");
    } else {
      auditService.logUpdate(
          teamId,
          "DEPOSIT",
          saved.getId(),
          principal.getUserId(),
          before,
          saved,
          auditService.getChangedFields(before, saved));
    }
    return toResponse(saved, contract);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public DepositResponse addDeduction(
      ContractIdentifier contractIdentifier,
      CreateDepositDeductionRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    Deposit deposit = requireDeposit(contract, teamId);
    if (deposit.getStatus() == DepositStatus.RETURNED
        || deposit.getStatus() == DepositStatus.FORFEITED) {
      throw new BusinessRuleException("Deductions cannot be added once the deposit is closed");
    }
    BigDecimal refundable = refundable(deposit, teamId);
    if (request.amount().compareTo(refundable) > 0) {
      throw new BusinessRuleException(
          "Deduction " + request.amount() + " exceeds the refundable amount " + refundable);
    }
    DepositDeduction deduction =
        DepositDeduction.builder()
            .identifier(Optional.of(newDepositDeductionId()))
            .teamId(teamId)
            .depositId(deposit.getId())
            .amount(MoneyAmount.of(request.amount(), deposit.getAmount().currency()))
            .reason(request.reason())
            .deductionDate(request.deductionDate().orElse(LocalDate.now(clock)))
            .createdBy(principal.getUserId())
            .updatedBy(principal.getUserId())
            .build();
    depositRepository.saveDeduction(deduction);
    auditService.logUpdate(
        teamId,
        "DEPOSIT",
        deposit.getId(),
        principal.getUserId(),
        null,
        deposit,
        Map.of("deductionAdded", request.amount() + ": " + request.reason()));
    metricsService.incrementCounter("deposit.deduction.total");
    return toResponse(deposit, contract);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public DepositResponse removeDeduction(
      ContractIdentifier contractIdentifier,
      DepositDeductionIdentifier deductionIdentifier,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    Deposit deposit = requireDeposit(contract, teamId);
    if (deposit.getStatus() == DepositStatus.RETURNED
        || deposit.getStatus() == DepositStatus.FORFEITED) {
      throw new BusinessRuleException("Deductions cannot be changed once the deposit is closed");
    }
    if (!depositRepository.softDeleteDeduction(deductionIdentifier, deposit.getId(), teamId)) {
      throw new NotFoundException("Deduction not found");
    }
    auditService.logUpdate(
        teamId,
        "DEPOSIT",
        deposit.getId(),
        principal.getUserId(),
        null,
        deposit,
        Map.of("deductionRemoved", deductionIdentifier.value()));
    return toResponse(deposit, contract);
  }

  /**
   * Records money paid back to the tenant. Anything below the refundable amount is a partial
   * return.
   */
  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public DepositResponse returnDeposit(
      ContractIdentifier contractIdentifier,
      ReturnDepositRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    Deposit deposit = requireDeposit(contract, teamId);
    if (deposit.getStatus() == DepositStatus.EXPECTED) {
      throw new BusinessRuleException("The deposit was never received, so it cannot be returned");
    }
    if (deposit.getStatus() == DepositStatus.RETURNED
        || deposit.getStatus() == DepositStatus.FORFEITED) {
      throw new BusinessRuleException("Deposit is already closed");
    }
    BigDecimal refundable = refundable(deposit, teamId);
    BigDecimal amount = request.returnedAmount().orElse(refundable);
    if (amount.compareTo(refundable) > 0) {
      throw new BusinessRuleException(
          "Returned amount " + amount + " exceeds the refundable amount " + refundable);
    }
    Deposit before = deposit.toBuilder().build();
    deposit.setReturnedAmount(deposit.getReturnedAmount().add(amount));
    deposit.setReturnedDate(Optional.of(request.returnedDate()));
    deposit.setStatus(
        amount.compareTo(refundable) == 0
            ? DepositStatus.RETURNED
            : DepositStatus.PARTIALLY_RETURNED);
    request.notes().filter(n -> !n.isBlank()).ifPresent(n -> deposit.setNotes(Optional.of(n)));
    deposit.setUpdatedBy(principal.getUserId());
    deposit.setUpdatedAt(clock.instant());
    Deposit saved = depositRepository.save(deposit);
    auditService.logUpdate(
        teamId,
        "DEPOSIT",
        saved.getId(),
        principal.getUserId(),
        before,
        saved,
        Map.of(
            "returned",
            amount + " " + saved.getAmount().currency() + " on " + request.returnedDate()));
    metricsService.incrementCounter("deposit.returned.total", "status", saved.getStatus().name());
    log.info(
        "Returned {} {} of deposit {} for contract {}",
        amount,
        saved.getAmount().currency(),
        saved.getIdentifier().orElseThrow(),
        contractIdentifier);
    return toResponse(saved, contract);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public DepositResponse forfeit(
      ContractIdentifier contractIdentifier,
      ForfeitDepositRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    Deposit deposit = requireDeposit(contract, teamId);
    if (deposit.getStatus() == DepositStatus.RETURNED
        || deposit.getStatus() == DepositStatus.FORFEITED) {
      throw new BusinessRuleException("Deposit is already closed");
    }
    Deposit before = deposit.toBuilder().build();
    deposit.setStatus(DepositStatus.FORFEITED);
    deposit.setNotes(Optional.of("Forfeited: " + request.reason()));
    deposit.setUpdatedBy(principal.getUserId());
    deposit.setUpdatedAt(clock.instant());
    Deposit saved = depositRepository.save(deposit);
    auditService.logUpdate(
        teamId,
        "DEPOSIT",
        saved.getId(),
        principal.getUserId(),
        before,
        saved,
        Map.of("forfeited", request.reason()));
    metricsService.incrementCounter("deposit.forfeited.total");
    return toResponse(saved, contract);
  }

  private Deposit requireDeposit(Contract contract, UUID teamId) {
    return depositRepository
        .findByContractIdAndTeamId(contract.getId(), teamId)
        .orElseThrow(() -> new NotFoundException("No deposit recorded for this contract"));
  }

  private BigDecimal deductionsTotal(Deposit deposit, UUID teamId) {
    return depositRepository.findDeductions(deposit.getId(), teamId).stream()
        .map(d -> d.getAmount().value())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  BigDecimal refundable(Deposit deposit, UUID teamId) {
    return deposit
        .getAmount()
        .value()
        .subtract(deductionsTotal(deposit, teamId))
        .subtract(deposit.getReturnedAmount())
        .max(BigDecimal.ZERO);
  }

  DepositResponse toResponse(Deposit deposit, Contract contract) {
    List<DepositDeduction> deductions =
        depositRepository.findDeductions(deposit.getId(), deposit.getTeamId());
    BigDecimal deductionsTotal =
        deductions.stream()
            .map(d -> d.getAmount().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal refundable =
        deposit
            .getAmount()
            .value()
            .subtract(deductionsTotal)
            .subtract(deposit.getReturnedAmount())
            .max(BigDecimal.ZERO);
    return new DepositResponse(
        deposit.getIdentifier().orElseThrow(),
        contract.getIdentifier().orElseThrow(),
        deposit.getAmount().value(),
        deposit.getAmount().currency(),
        deposit.getReceivedDate(),
        deposit.getHeldWhere(),
        deposit.getStatus(),
        deposit.getReturnDueDate(),
        deposit.getReturnedDate(),
        deposit.getReturnedAmount(),
        deductionsTotal,
        refundable,
        deposit.getNotes(),
        deductions.stream()
            .map(
                d ->
                    new DepositDeductionResponse(
                        d.getIdentifier().orElseThrow(),
                        d.getAmount().value(),
                        d.getAmount().currency(),
                        d.getReason(),
                        d.getDeductionDate()))
            .toList(),
        deposit.getCreatedAt(),
        Optional.ofNullable(deposit.getUpdatedAt()));
  }
}
