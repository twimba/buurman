package com.buurman.service;

import static com.buurman.util.UlidGenerator.newFinancingPaymentId;
import static java.util.stream.Collectors.joining;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.buurman.domain.Ulid;
import com.buurman.domain.identifier.FinancingPaymentIdentifier;
import com.buurman.domain.identifier.PropertyFinancingIdentifier;
import com.buurman.domain.FinancingPayment;
import com.buurman.domain.PropertyFinancing;
import com.buurman.dto.request.CreateFinancingPaymentRequest;
import com.buurman.dto.request.UpdateFinancingPaymentRequest;
import com.buurman.dto.response.BulkCreateResult;
import com.buurman.dto.response.DocumentResponse;
import com.buurman.dto.response.FinancingPaymentResponse;
import com.buurman.mapper.FinancingPaymentMapper;
import com.buurman.repository.FinancingPaymentRepository;
import com.buurman.repository.PropertyFinancingRepository;
import com.buurman.security.UserPrincipal;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FinancingPaymentService {

  private final FinancingPaymentRepository paymentRepository;
  private final PropertyFinancingRepository financingRepository;
  private final FinancingPaymentMapper paymentMapper;
  private final DocumentService documentService;
  private final PlatformTransactionManager transactionManager;
  private final Validator validator;
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public FinancingPaymentResponse create(
      PropertyFinancingIdentifier financingIdentifier, CreateFinancingPaymentRequest request, UserPrincipal principal) {
    return performCreate(financingIdentifier, request, principal);
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<BulkCreateResult<FinancingPaymentResponse>> bulkCreate(
      PropertyFinancingIdentifier financingIdentifier,
      List<CreateFinancingPaymentRequest> requests,
      UserPrincipal principal) {

    TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
    List<BulkCreateResult<FinancingPaymentResponse>> results = new ArrayList<>();

    for (int i = 0; i < requests.size(); i++) {
      final int index = i;
      CreateFinancingPaymentRequest request = requests.get(i);

      Set<ConstraintViolation<CreateFinancingPaymentRequest>> violations =
          validator.validate(request);
      if (!violations.isEmpty()) {
        String errorMsg =
            violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(joining(", "));
        results.add(BulkCreateResult.error(index, errorMsg));
        continue;
      }

      try {
        FinancingPaymentResponse response =
            txTemplate.execute(status -> performCreate(financingIdentifier, request, principal));
        results.add(BulkCreateResult.success(index, response));
      } catch (Exception e) {
        log.warn("Bulk financing payment creation failed for item {}: {}", index, e.getMessage());
        results.add(BulkCreateResult.error(index, extractErrorMessage(e)));
      }
    }

    return results;
  }

  private FinancingPaymentResponse performCreate(
      PropertyFinancingIdentifier financingIdentifier, CreateFinancingPaymentRequest request, UserPrincipal principal) {

    PropertyFinancing financing =
        financingRepository.getByIdentifierAndTeamId(
            financingIdentifier, principal.requireTeamId());

    FinancingPayment payment = paymentMapper.toEntity(request);
    payment.setFinancingId(financing.getId());
    payment.setIdentifier(Optional.of(newFinancingPaymentId()));
    payment.setTeamId(principal.requireTeamId());
    payment.setCreatedBy(principal.getUserId());
    payment.setUpdatedBy(principal.getUserId());
    payment.setCreatedAt(clock.instant());
    payment.setUpdatedAt(clock.instant());

    boolean shouldDeduct = request.deductFromBalance().orElse(true);
    BigDecimal deductionAmount = computeDeductionAmount(payment);

    if (shouldDeduct && deductionAmount.compareTo(BigDecimal.ZERO) > 0) {
      adjustFinancingBalance(financing, deductionAmount, principal.getUserId());
      payment.setBalanceDeducted(true);
    }

    FinancingPayment saved = paymentRepository.save(payment);

    log.info(
        "Created financing payment {} for financing {} by user {}{}",
        saved.getIdentifier().orElseThrow(),
        financing.getIdentifier().orElseThrow(),
        principal.getUserId(),
        saved.isBalanceDeducted() ? " (balance deducted by " + deductionAmount + ")" : "");

    return toResponse(saved, financing.getIdentifier().orElseThrow());
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public FinancingPaymentResponse update(
      FinancingPaymentIdentifier paymentIdentifier, UpdateFinancingPaymentRequest request, UserPrincipal principal) {

    FinancingPayment payment =
        paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, principal.requireTeamId());

    BigDecimal oldDeduction =
        payment.isBalanceDeducted() ? computeDeductionAmount(payment) : BigDecimal.ZERO;

    paymentMapper.updateEntity(payment, request);
    payment.setUpdatedBy(principal.getUserId());
    payment.setUpdatedAt(clock.instant());

    boolean shouldDeduct = request.deductFromBalance().orElse(payment.isBalanceDeducted());

    if (shouldDeduct) {
      BigDecimal newDeduction = computeDeductionAmount(payment);
      BigDecimal delta = newDeduction.subtract(oldDeduction);
      if (delta.compareTo(BigDecimal.ZERO) != 0) {
        PropertyFinancing financing =
            financingRepository
                .findByIdAndTeamId(payment.getFinancingId(), principal.requireTeamId())
                .orElseThrow(
                    () -> new com.buurman.exception.NotFoundException("Financing not found"));
        adjustFinancingBalance(financing, delta, principal.getUserId());
      }
      payment.setBalanceDeducted(true);
    } else if (payment.isBalanceDeducted()) {
      // Was previously deducted, now opted out — reverse
      if (oldDeduction.compareTo(BigDecimal.ZERO) > 0) {
        PropertyFinancing financing =
            financingRepository
                .findByIdAndTeamId(payment.getFinancingId(), principal.requireTeamId())
                .orElseThrow(
                    () -> new com.buurman.exception.NotFoundException("Financing not found"));
        adjustFinancingBalance(financing, oldDeduction.negate(), principal.getUserId());
      }
      payment.setBalanceDeducted(false);
    }

    FinancingPayment updated = paymentRepository.save(payment);

    log.info(
        "Updated financing payment {} by user {}", updated.getIdentifier().orElseThrow(), principal.getUserId());

    Ulid financingIdentifier =
        resolveFinancingIdentifier(updated.getFinancingId(), principal.requireTeamId());

    return toResponse(updated, financingIdentifier);
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void delete(FinancingPaymentIdentifier paymentIdentifier, UserPrincipal principal) {
    FinancingPayment payment =
        paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, principal.requireTeamId());

    // Reverse balance deduction if applicable
    if (payment.isBalanceDeducted()) {
      BigDecimal deduction = computeDeductionAmount(payment);
      if (deduction.compareTo(BigDecimal.ZERO) > 0) {
        PropertyFinancing financing =
            financingRepository
                .findByIdAndTeamId(payment.getFinancingId(), principal.requireTeamId())
                .orElseThrow(
                    () -> new com.buurman.exception.NotFoundException("Financing not found"));
        adjustFinancingBalance(financing, deduction.negate(), principal.getUserId());
      }
    }

    paymentRepository.softDeleteByIdAndTeamId(payment.getId(), principal.requireTeamId());

    log.info(
        "Deleted financing payment {} by user {}", payment.getIdentifier().orElseThrow(), principal.getUserId());
  }

  @Transactional(readOnly = true)
  public List<FinancingPaymentResponse> listByFinancing(
      PropertyFinancingIdentifier financingIdentifier, UserPrincipal principal) {

    PropertyFinancing financing =
        financingRepository.getByIdentifierAndTeamId(
            financingIdentifier, principal.requireTeamId());

    return paymentRepository
        .findByFinancingIdAndTeamId(financing.getId(), principal.requireTeamId())
        .stream()
        .map(p -> toResponse(p, financing.getIdentifier().orElseThrow()))
        .toList();
  }

  // ===== Documents =====

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public DocumentResponse uploadPaymentDocument(
      FinancingPaymentIdentifier paymentIdentifier,
      MultipartFile file,
      @Nullable String title,
      @Nullable String notes,
      UserPrincipal principal) {
    FinancingPayment payment =
        paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, principal.requireTeamId());

    return documentService.uploadDocument(
        file,
        "FINANCING_PAYMENT",
        payment.getId(),
        payment.getIdentifier().orElseThrow(),
        title,
        notes,
        principal);
  }

  public List<DocumentResponse> getPaymentDocuments(
      FinancingPaymentIdentifier paymentIdentifier, UserPrincipal principal) {
    FinancingPayment payment =
        paymentRepository.getByIdentifierAndTeamId(paymentIdentifier, principal.requireTeamId());

    return documentService.getDocuments("FINANCING_PAYMENT", payment.getId(), principal);
  }

  // ===== Balance Helpers =====

  private BigDecimal computeDeductionAmount(FinancingPayment payment) {
    BigDecimal principal = payment.getPrincipalAmount().orElse(BigDecimal.ZERO);
    BigDecimal extra = payment.getExtraPayment().orElse(BigDecimal.ZERO);
    return principal.add(extra);
  }

  private void adjustFinancingBalance(PropertyFinancing financing, BigDecimal amount, UUID userId) {
    BigDecimal current = financing.getCurrentBalance().orElse(financing.getOriginalAmount());
    BigDecimal newBalance = current.subtract(amount);
    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
      newBalance = BigDecimal.ZERO;
    }
    financing.setCurrentBalance(Optional.of(newBalance));
    if (financing.getCurrentBalanceCurrency().isEmpty()) {
      financing.setCurrentBalanceCurrency(Optional.of(financing.getOriginalAmountCurrency()));
    }
    financing.setUpdatedBy(userId);
    financing.setUpdatedAt(clock.instant());
    financingRepository.save(financing);
  }

  // ===== Response Helpers =====

  private FinancingPaymentResponse toResponse(
      FinancingPayment payment, Ulid financingIdentifier) {

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
        mapped.balanceDeducted(),
        mapped.createdAt(),
        mapped.updatedAt());
  }

  private Ulid resolveFinancingIdentifier(UUID financingId, UUID teamId) {
    return financingRepository
        .findByIdAndTeamId(financingId, teamId)
        .flatMap(PropertyFinancing::getIdentifier)
        .orElseThrow();
  }

  private String extractErrorMessage(Exception e) {
    String message = e.getMessage();
    if (message == null || message.isBlank()) {
      return "An unexpected error occurred";
    }
    return message;
  }
}
