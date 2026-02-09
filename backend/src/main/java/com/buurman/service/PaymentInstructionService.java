package com.buurman.service;

import com.buurman.domain.PaymentInstruction;
import com.buurman.dto.request.CreatePaymentInstructionRequest;
import com.buurman.dto.request.UpdatePaymentInstructionRequest;
import com.buurman.dto.response.PaymentInstructionResponse;
import com.buurman.mapper.PaymentInstructionMapper;
import com.buurman.repository.PaymentInstructionRepository;
import com.buurman.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PaymentInstructionService {

    private static final Logger log = LoggerFactory.getLogger(PaymentInstructionService.class);

    private final PaymentInstructionRepository repository;
    private final PaymentInstructionMapper mapper;
    private final AuditService auditService;

    public PaymentInstructionService(
            PaymentInstructionRepository repository,
            PaymentInstructionMapper mapper,
            AuditService auditService) {
        this.repository = repository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    public List<PaymentInstructionResponse> getAll(UserPrincipal principal) {
        return repository.findAllByTeamId(principal.getTeamId()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public PaymentInstructionResponse getByIdentifier(String identifier, UserPrincipal principal) {
        PaymentInstruction pi = repository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Payment instruction not found"));
        return mapper.toResponse(pi);
    }

    @Transactional
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public PaymentInstructionResponse create(CreatePaymentInstructionRequest request, UserPrincipal principal) {
        PaymentInstruction pi = mapper.toEntity(request);
        pi.setTeamId(principal.getTeamId());
        pi.setCreatedBy(principal.getUserId());
        pi.setUpdatedBy(principal.getUserId());
        pi.setCreatedAt(Instant.now());
        pi.setUpdatedAt(Instant.now());

        if (Boolean.TRUE.equals(request.isDefault())) {
            repository.clearDefaultByTeamId(principal.getTeamId());
        }

        PaymentInstruction saved = repository.save(pi);
        log.info("Payment instruction created: {} in team {}", saved.getIdentifier(), principal.getTeamId());

        auditService.logCreate(
                principal.getTeamId(),
                "PAYMENT_INSTRUCTION",
                saved.getId(),
                principal.getUserId(),
                saved
        );

        return mapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public PaymentInstructionResponse update(String identifier, UpdatePaymentInstructionRequest request, UserPrincipal principal) {
        PaymentInstruction pi = repository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Payment instruction not found"));

        // Store old values for audit
        String oldName = pi.getName();
        String oldPaymentMethod = pi.getPaymentMethod() != null ? pi.getPaymentMethod().name() : null;
        Boolean oldIsDefault = pi.getIsDefault();

        if (Boolean.TRUE.equals(request.isDefault()) && !Boolean.TRUE.equals(pi.getIsDefault())) {
            repository.clearDefaultByTeamId(principal.getTeamId());
        }

        mapper.updateEntity(pi, request);
        pi.setUpdatedBy(principal.getUserId());
        pi.setUpdatedAt(Instant.now());

        PaymentInstruction updated = repository.save(pi);
        log.info("Payment instruction updated: {} in team {}", identifier, principal.getTeamId());

        Map<String, Object> changedFields = new HashMap<>();
        if (!Objects.equals(oldName, updated.getName())) {
            changedFields.put("name", updated.getName());
        }
        String newPaymentMethod = updated.getPaymentMethod() != null ? updated.getPaymentMethod().name() : null;
        if (!Objects.equals(oldPaymentMethod, newPaymentMethod)) {
            changedFields.put("paymentMethod", newPaymentMethod);
        }
        if (!Objects.equals(oldIsDefault, updated.getIsDefault())) {
            changedFields.put("isDefault", updated.getIsDefault());
        }

        auditService.logUpdate(
                principal.getTeamId(),
                "PAYMENT_INSTRUCTION",
                updated.getId(),
                principal.getUserId(),
                null,
                updated,
                changedFields
        );

        return mapper.toResponse(updated);
    }

    @Transactional
    @PreAuthorize("hasRole('TEAM_ADMIN')")
    public void delete(String identifier, UserPrincipal principal) {
        PaymentInstruction pi = repository.findByIdentifierAndTeamId(identifier, principal.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Payment instruction not found"));

        repository.softDeleteByIdAndTeamId(pi.getId(), principal.getTeamId());
        log.info("Payment instruction deleted: {} in team {}", identifier, principal.getTeamId());

        auditService.logDelete(
                principal.getTeamId(),
                "PAYMENT_INSTRUCTION",
                pi.getId(),
                principal.getUserId(),
                pi
        );
    }
}
