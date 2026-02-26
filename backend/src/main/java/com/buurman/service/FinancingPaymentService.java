package com.buurman.service;

import static com.buurman.util.UlidGenerator.newFinancingPaymentId;

import java.time.Clock;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.FinancingPayment;
import com.buurman.domain.PropertyFinancing;
import com.buurman.dto.request.CreateFinancingPaymentRequest;
import com.buurman.dto.request.UpdateFinancingPaymentRequest;
import com.buurman.dto.response.FinancingPaymentResponse;
import com.buurman.mapper.FinancingPaymentMapper;
import com.buurman.repository.FinancingPaymentRepository;
import com.buurman.repository.PropertyFinancingRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FinancingPaymentService {

  private final FinancingPaymentRepository paymentRepository;
  private final PropertyFinancingRepository financingRepository;
  private final FinancingPaymentMapper paymentMapper;
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public FinancingPaymentResponse create(
      String financingIdentifier, CreateFinancingPaymentRequest request, UserPrincipal principal) {

    PropertyFinancing financing =
        financingRepository.getByIdentifierAndTeamId(
            financingIdentifier, principal.requireTeamId());

    FinancingPayment payment = paymentMapper.toEntity(request);
    payment.setFinancingId(financing.getId());
    payment.setIdentifier(newFinancingPaymentId().value());
    payment.setTeamId(principal.requireTeamId());
    payment.setCreatedBy(principal.getUserId());
    payment.setUpdatedBy(principal.getUserId());
    payment.setCreatedAt(clock.instant());
    payment.setUpdatedAt(clock.instant());

    FinancingPayment saved = paymentRepository.save(payment);

    log.info(
        "Created financing payment {} for financing {} by user {}",
        saved.getIdentifier(),
        financing.getIdentifier(),
        principal.getUserId());

    return toResponse(saved, financing.getIdentifier());
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public FinancingPaymentResponse update(
      String paymentIdentifier, UpdateFinancingPaymentRequest request, UserPrincipal principal) {

    FinancingPayment payment =
        paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, principal.requireTeamId());

    paymentMapper.updateEntity(payment, request);
    payment.setUpdatedBy(principal.getUserId());
    payment.setUpdatedAt(clock.instant());

    FinancingPayment updated = paymentRepository.save(payment);

    log.info(
        "Updated financing payment {} by user {}", updated.getIdentifier(), principal.getUserId());

    String financingIdentifier =
        resolveFinancingIdentifier(updated.getFinancingId(), principal.requireTeamId());

    return toResponse(updated, financingIdentifier);
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void delete(String paymentIdentifier, UserPrincipal principal) {
    FinancingPayment payment =
        paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, principal.requireTeamId());

    paymentRepository.softDeleteByIdAndTeamId(payment.getId(), principal.requireTeamId());

    log.info(
        "Deleted financing payment {} by user {}", payment.getIdentifier(), principal.getUserId());
  }

  @Transactional(readOnly = true)
  public List<FinancingPaymentResponse> listByFinancing(
      String financingIdentifier, UserPrincipal principal) {

    PropertyFinancing financing =
        financingRepository.getByIdentifierAndTeamId(
            financingIdentifier, principal.requireTeamId());

    return paymentRepository
        .findByFinancingIdAndTeamId(financing.getId(), principal.requireTeamId())
        .stream()
        .map(p -> toResponse(p, financing.getIdentifier()))
        .toList();
  }

  private FinancingPaymentResponse toResponse(
      FinancingPayment payment, String financingIdentifier) {

    FinancingPaymentResponse mapped = paymentMapper.toResponse(payment);
    return new FinancingPaymentResponse(
        mapped.identifier(),
        financingIdentifier,
        mapped.paymentDate(),
        mapped.totalAmount(),
        mapped.principalAmount(),
        mapped.interestAmount(),
        mapped.escrowAmount(),
        mapped.extraPayment(),
        mapped.currency(),
        mapped.status(),
        mapped.notes(),
        mapped.createdAt(),
        mapped.updatedAt());
  }

  private String resolveFinancingIdentifier(java.util.UUID financingId, java.util.UUID teamId) {
    return financingRepository
        .findByIdAndTeamId(financingId, teamId)
        .map(PropertyFinancing::getIdentifier)
        .orElse("UNKNOWN");
  }
}
