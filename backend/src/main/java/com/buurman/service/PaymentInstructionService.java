package com.buurman.service;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.PaymentInstruction;
import com.buurman.dto.request.CreatePaymentInstructionRequest;
import com.buurman.dto.request.UpdatePaymentInstructionRequest;
import com.buurman.dto.response.PaymentInstructionResponse;
import com.buurman.mapper.PaymentInstructionMapper;
import com.buurman.repository.PaymentInstructionRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentInstructionService {

  private final PaymentInstructionRepository repository;
  private final PaymentInstructionMapper mapper;
  private final AuditService auditService;
  private final Clock clock;

  public List<PaymentInstructionResponse> getAll(UserPrincipal principal) {
    return repository.findAllByTeamId(principal.requireTeamId()).stream()
        .map(mapper::toResponse)
        .toList();
  }

  public PaymentInstructionResponse getByIdentifier(String identifier, UserPrincipal principal) {
    PaymentInstruction pi =
        repository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    return mapper.toResponse(pi);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentInstructionResponse create(
      CreatePaymentInstructionRequest request, UserPrincipal principal) {
    PaymentInstruction pi = mapper.toEntity(request);
    pi.setTeamId(principal.requireTeamId());
    pi.setCreatedBy(principal.getUserId());
    pi.setUpdatedBy(principal.getUserId());
    pi.setCreatedAt(clock.instant());
    pi.setUpdatedAt(clock.instant());

    if (request.isDefault().orElse(false)) {
      repository.clearDefaultByTeamId(principal.requireTeamId());
    }

    PaymentInstruction saved = repository.save(pi);
    log.info(
        "Payment instruction created: {} in team {}",
        saved.getIdentifier(),
        principal.requireTeamId());

    auditService.logCreate(
        principal.requireTeamId(),
        "PAYMENT_INSTRUCTION",
        saved.getId(),
        principal.getUserId(),
        saved);

    return mapper.toResponse(saved);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public PaymentInstructionResponse update(
      String identifier, UpdatePaymentInstructionRequest request, UserPrincipal principal) {
    PaymentInstruction pi =
        repository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    // Store old values for audit
    String oldName = pi.getName();
    @Nullable String oldPaymentMethod =
        pi.getPaymentMethod() != null ? pi.getPaymentMethod().name() : null;
    Boolean oldIsDefault = pi.getIsDefault();

    if (request.isDefault().orElse(false) && !pi.getIsDefault()) {
      repository.clearDefaultByTeamId(principal.requireTeamId());
    }

    mapper.updateEntity(pi, request);
    pi.setUpdatedBy(principal.getUserId());
    pi.setUpdatedAt(clock.instant());

    PaymentInstruction updated = repository.save(pi);
    log.info("Payment instruction updated: {} in team {}", identifier, principal.requireTeamId());

    Map<String, Object> changedFields = new HashMap<>();
    if (!Objects.equals(oldName, updated.getName())) {
      changedFields.put("name", updated.getName());
    }
    @Nullable String newPaymentMethod =
        updated.getPaymentMethod() != null ? updated.getPaymentMethod().name() : null;
    if (!Objects.equals(oldPaymentMethod, newPaymentMethod)) {
      changedFields.put("paymentMethod", newPaymentMethod);
    }
    if (!Objects.equals(oldIsDefault, updated.getIsDefault())) {
      changedFields.put("isDefault", updated.getIsDefault());
    }

    auditService.logUpdate(
        principal.requireTeamId(),
        "PAYMENT_INSTRUCTION",
        updated.getId(),
        principal.getUserId(),
        null,
        updated,
        changedFields);

    return mapper.toResponse(updated);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public void delete(String identifier, UserPrincipal principal) {
    PaymentInstruction pi =
        repository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());

    repository.softDeleteByIdAndTeamId(pi.getId(), principal.requireTeamId());
    log.info("Payment instruction deleted: {} in team {}", identifier, principal.requireTeamId());

    auditService.logDelete(
        principal.requireTeamId(), "PAYMENT_INSTRUCTION", pi.getId(), principal.getUserId(), pi);
  }
}
