package com.buurman.service;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractPaymentInstruction;
import com.buurman.domain.PaymentInstruction;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.ContractPaymentInstructionIdentifier;
import com.buurman.dto.request.CreateContractPaymentInstructionRequest;
import com.buurman.dto.request.UpdateContractPaymentInstructionRequest;
import com.buurman.dto.response.ContractPaymentInstructionResponse;
import com.buurman.repository.ContractPaymentInstructionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PaymentInstructionRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractPaymentInstructionService {

  private final ContractPaymentInstructionRepository cpiRepository;
  private final ContractRepository contractRepository;
  private final PaymentInstructionRepository piRepository;
  private final AuditService auditService;
  private final Clock clock;

  public List<ContractPaymentInstructionResponse> getHistory(
      ContractIdentifier contractIdentifier, UserPrincipal principal) {
    Contract contract = resolveContract(contractIdentifier, principal);
    List<ContractPaymentInstruction> history =
        cpiRepository.findByContractIdAndTeamId(contract.getId(), principal.requireTeamId());

    // Batch-load templates to avoid N+1
    List<UUID> templateIds =
        history.stream()
            .filter(cpi -> !cpi.getIsCustom())
            .flatMap(cpi -> cpi.getPaymentInstructionId().stream())
            .distinct()
            .toList();

    Map<UUID, PaymentInstruction> templateMap =
        templateIds.isEmpty()
            ? Map.of()
            : piRepository.findAllByTeamId(principal.requireTeamId()).stream()
                .filter(pi -> templateIds.contains(pi.getId()))
                .collect(toMap(PaymentInstruction::getId, identity()));

    return history.stream().map(cpi -> toResolvedResponse(cpi, templateMap)).toList();
  }

  public Optional<ContractPaymentInstructionResponse> getCurrent(
      ContractIdentifier contractIdentifier, UserPrincipal principal) {
    Contract contract = resolveContract(contractIdentifier, principal);
    return cpiRepository
        .findCurrentByContractIdAndTeamId(contract.getId(), principal.requireTeamId())
        .map(
            cpi -> {
              Map<UUID, PaymentInstruction> templateMap = Map.of();
              if (!cpi.getIsCustom()) {
                templateMap =
                    cpi.getPaymentInstructionId()
                        .flatMap(
                            piId -> piRepository.findByIdAndTeamId(piId, principal.requireTeamId()))
                        .map(pi -> Map.of(pi.getId(), pi))
                        .orElse(Map.of());
              }
              return toResolvedResponse(cpi, templateMap);
            });
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractPaymentInstructionResponse create(
      ContractIdentifier contractIdentifier,
      CreateContractPaymentInstructionRequest request,
      UserPrincipal principal) {

    Contract contract = resolveContract(contractIdentifier, principal);
    validateEffectiveFrom(request.effectiveFrom(), contract);

    // Auto-close or replace current entry if one exists
    cpiRepository
        .findCurrentByContractIdAndTeamId(contract.getId(), principal.requireTeamId())
        .ifPresent(
            current -> {
              if (!current.getEffectiveFrom().isBefore(request.effectiveFrom())) {
                // Current entry hasn't started yet or starts same day — replace it
                cpiRepository.softDeleteByIdAndTeamId(current.getId(), principal.requireTeamId());
                log.info(
                    "Replaced future payment instruction {} for contract {}",
                    current.getIdentifier().orElseThrow(),
                    contractIdentifier);
              } else {
                cpiRepository.setEffectiveTo(
                    current.getId(),
                    principal.requireTeamId(),
                    request.effectiveFrom().minusDays(1),
                    principal.getUserId());
                log.info(
                    "Closed previous payment instruction {} for contract {}",
                    current.getIdentifier().orElseThrow(),
                    contractIdentifier);
              }
            });

    ContractPaymentInstruction cpi = buildFromRequest(request, contract, principal);
    ContractPaymentInstruction saved = cpiRepository.save(cpi);
    log.info(
        "Contract payment instruction created: {} for contract {} in team {}",
        saved.getIdentifier().orElseThrow(),
        contractIdentifier,
        principal.requireTeamId());

    auditService.logCreate(
        principal.requireTeamId(),
        "CONTRACT_PAYMENT_INSTRUCTION",
        saved.getId(),
        principal.getUserId(),
        saved);

    return toResolvedResponse(saved, loadTemplateMap(saved, principal));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public ContractPaymentInstructionResponse update(
      ContractIdentifier contractIdentifier,
      ContractPaymentInstructionIdentifier instructionIdentifier,
      UpdateContractPaymentInstructionRequest request,
      UserPrincipal principal) {

    Contract contract = resolveContract(contractIdentifier, principal);
    validateEffectiveFrom(request.effectiveFrom(), contract);

    ContractPaymentInstruction existing =
        cpiRepository.getByIdentifierAndTeamId(instructionIdentifier, principal.requireTeamId());

    if (!existing.getContractId().equals(contract.getId())) {
      throw new IllegalArgumentException(
          "Payment instruction does not belong to the specified contract");
    }

    // Close or replace the existing entry
    if (!existing.getEffectiveFrom().isBefore(request.effectiveFrom())) {
      // Existing entry hasn't started yet or starts same day — replace it
      cpiRepository.softDeleteByIdAndTeamId(existing.getId(), principal.requireTeamId());
    } else {
      cpiRepository.setEffectiveTo(
          existing.getId(),
          principal.requireTeamId(),
          request.effectiveFrom().minusDays(1),
          principal.getUserId());
    }

    // Create a new entry (append-only history)
    ContractPaymentInstruction cpi = new ContractPaymentInstruction();
    cpi.setTeamId(principal.requireTeamId());
    cpi.setContractId(contract.getId());
    cpi.setEffectiveFrom(request.effectiveFrom());
    cpi.setNotes(request.notes());
    cpi.setCreatedBy(principal.getUserId());
    cpi.setUpdatedBy(principal.getUserId());
    cpi.setCreatedAt(clock.instant());
    cpi.setUpdatedAt(clock.instant());

    if (request.paymentInstructionIdentifier().isPresent()
        && !request.isCustom().map(Boolean.TRUE::equals).orElse(false)) {
      PaymentInstruction template =
          piRepository.getByIdentifierAndTeamId(
              Sid.of(request.paymentInstructionIdentifier().get()), principal.requireTeamId());
      cpi.setPaymentInstructionId(Optional.of(template.getId()));
      cpi.setIsCustom(false);
    } else {
      cpi.setIsCustom(true);
      cpi.setCustomName(request.customName());
      cpi.setCustomDescription(request.customDescription());
      cpi.setCustomPaymentMethod(request.customPaymentMethod());
      cpi.setCustomBankName(request.customBankName());
      cpi.setCustomAccountHolderName(request.customAccountHolderName());
      cpi.setCustomIban(request.customIban());
      cpi.setCustomBicSwift(request.customBicSwift());
      cpi.setCustomAccountNumber(request.customAccountNumber());
      cpi.setCustomRoutingNumber(request.customRoutingNumber());
      cpi.setCustomPaymentReference(request.customPaymentReference());
      cpi.setCustomAdditionalDetails(request.customAdditionalDetails());
    }

    ContractPaymentInstruction saved = cpiRepository.save(cpi);
    log.info(
        "Contract payment instruction updated (new entry): {} for contract {} in team {}",
        saved.getIdentifier().orElseThrow(),
        contractIdentifier,
        principal.requireTeamId());

    auditService.logCreate(
        principal.requireTeamId(),
        "CONTRACT_PAYMENT_INSTRUCTION",
        saved.getId(),
        principal.getUserId(),
        saved);

    return toResolvedResponse(saved, loadTemplateMap(saved, principal));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public void delete(
      ContractIdentifier contractIdentifier,
      ContractPaymentInstructionIdentifier instructionIdentifier,
      UserPrincipal principal) {
    Contract contract = resolveContract(contractIdentifier, principal);
    ContractPaymentInstruction cpi =
        cpiRepository.getByIdentifierAndTeamId(instructionIdentifier, principal.requireTeamId());

    if (!cpi.getContractId().equals(contract.getId())) {
      throw new IllegalArgumentException(
          "Payment instruction does not belong to the specified contract");
    }

    if (contract.getStatus() != Contract.ContractStatus.DRAFT
        && cpi.getEffectiveFrom() != null
        && cpi.getEffectiveFrom().isBefore(LocalDate.now(clock))) {
      throw new IllegalArgumentException(
          "Cannot delete a payment instruction that was already effective");
    }

    cpiRepository.softDeleteByIdAndTeamId(cpi.getId(), principal.requireTeamId());
    log.info(
        "Contract payment instruction deleted: {} for contract {} in team {}",
        instructionIdentifier,
        contractIdentifier,
        principal.requireTeamId());

    auditService.logDelete(
        principal.requireTeamId(),
        "CONTRACT_PAYMENT_INSTRUCTION",
        cpi.getId(),
        principal.getUserId(),
        cpi);
  }

  private Contract resolveContract(ContractIdentifier contractIdentifier, UserPrincipal principal) {
    return contractRepository.getByIdentifierAndTeamId(
        contractIdentifier, principal.requireTeamId());
  }

  private void validateEffectiveFrom(LocalDate effectiveFrom, Contract contract) {
    LocalDate minDate;
    if (contract.getStatus() == Contract.ContractStatus.DRAFT) {
      minDate =
          Stream.of(
                  Optional.of(LocalDate.now(clock)),
                  Optional.of(contract.getStartDate()),
                  contract.getSignedDate())
              .filter(Optional::isPresent)
              .map(Optional::get)
              .min(LocalDate::compareTo)
              .orElse(LocalDate.now(clock));
    } else {
      minDate = LocalDate.now(clock);
    }
    if (effectiveFrom.isBefore(minDate)) {
      throw new IllegalArgumentException("Effective from date must not be before " + minDate);
    }
  }

  private ContractPaymentInstruction buildFromRequest(
      CreateContractPaymentInstructionRequest request, Contract contract, UserPrincipal principal) {

    ContractPaymentInstruction cpi = new ContractPaymentInstruction();
    cpi.setTeamId(principal.requireTeamId());
    cpi.setContractId(contract.getId());
    cpi.setEffectiveFrom(request.effectiveFrom());
    cpi.setNotes(request.notes());
    cpi.setCreatedBy(principal.getUserId());
    cpi.setUpdatedBy(principal.getUserId());
    cpi.setCreatedAt(clock.instant());
    cpi.setUpdatedAt(clock.instant());

    if (request.paymentInstructionIdentifier().isPresent()
        && !request.isCustom().map(Boolean.TRUE::equals).orElse(false)) {
      PaymentInstruction template =
          piRepository.getByIdentifierAndTeamId(
              Sid.of(request.paymentInstructionIdentifier().get()), principal.requireTeamId());
      cpi.setPaymentInstructionId(Optional.of(template.getId()));
      cpi.setIsCustom(false);
    } else {
      cpi.setIsCustom(true);
      cpi.setCustomName(request.customName());
      cpi.setCustomDescription(request.customDescription());
      cpi.setCustomPaymentMethod(request.customPaymentMethod());
      cpi.setCustomBankName(request.customBankName());
      cpi.setCustomAccountHolderName(request.customAccountHolderName());
      cpi.setCustomIban(request.customIban());
      cpi.setCustomBicSwift(request.customBicSwift());
      cpi.setCustomAccountNumber(request.customAccountNumber());
      cpi.setCustomRoutingNumber(request.customRoutingNumber());
      cpi.setCustomPaymentReference(request.customPaymentReference());
      cpi.setCustomAdditionalDetails(request.customAdditionalDetails());
    }

    return cpi;
  }

  private Map<UUID, PaymentInstruction> loadTemplateMap(
      ContractPaymentInstruction cpi, UserPrincipal principal) {
    if (!cpi.getIsCustom()) {
      return cpi.getPaymentInstructionId()
          .flatMap(piId -> piRepository.findByIdAndTeamId(piId, principal.requireTeamId()))
          .map(pi -> Map.of(pi.getId(), pi))
          .orElse(Map.of());
    }
    return Map.of();
  }

  private ContractPaymentInstructionResponse toResolvedResponse(
      ContractPaymentInstruction cpi, Map<UUID, PaymentInstruction> templateMap) {

    Optional<PaymentInstruction> maybeTemplate =
        cpi.getIsCustom() ? Optional.empty() : cpi.getPaymentInstructionId().map(templateMap::get);

    if (maybeTemplate.isPresent()) {
      PaymentInstruction template = maybeTemplate.get();
      return new ContractPaymentInstructionResponse(
          cpi.getIdentifier().orElseThrow(),
          Optional.of(template.getIdentifier().orElseThrow()),
          Optional.of(false),
          Optional.ofNullable(template.getName()),
          Optional.ofNullable(template.getDescription()),
          Optional.ofNullable(
              template.getPaymentMethod() != null ? template.getPaymentMethod().name() : null),
          template.getBankName(),
          template.getAccountHolderName(),
          template.getIban(),
          template.getBicSwift(),
          template.getAccountNumber(),
          template.getRoutingNumber(),
          template.getPaymentReference(),
          template.getAdditionalDetails(),
          Optional.of(cpi.getEffectiveFrom()),
          cpi.getEffectiveTo(),
          cpi.getNotes(),
          cpi.getCreatedAt(),
          Optional.of(cpi.getUpdatedAt()));
    }

    // Custom or template not found
    return new ContractPaymentInstructionResponse(
        cpi.getIdentifier().orElseThrow(),
        Optional.empty(),
        Optional.of(true),
        cpi.getCustomName(),
        cpi.getCustomDescription(),
        cpi.getCustomPaymentMethod(),
        cpi.getCustomBankName(),
        cpi.getCustomAccountHolderName(),
        cpi.getCustomIban(),
        cpi.getCustomBicSwift(),
        cpi.getCustomAccountNumber(),
        cpi.getCustomRoutingNumber(),
        cpi.getCustomPaymentReference(),
        cpi.getCustomAdditionalDetails(),
        Optional.of(cpi.getEffectiveFrom()),
        cpi.getEffectiveTo(),
        cpi.getNotes(),
        cpi.getCreatedAt(),
        Optional.of(cpi.getUpdatedAt()));
  }
}
