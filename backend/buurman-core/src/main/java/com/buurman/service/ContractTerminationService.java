package com.buurman.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractTermination;
import com.buurman.domain.ContractTerminationStatus;
import com.buurman.domain.Document;
import com.buurman.domain.Sid;
import com.buurman.domain.TerminationGivenBy;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.TerminateContractRequest;
import com.buurman.dto.response.ContractTerminationResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.ContractTerminationRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.regulation.TerminationRuleResolver;
import com.buurman.util.Constants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Terminates an ACTIVE contract: computes the legally-required notice period (via {@link
 * TerminationRuleResolver}), records the termination, transitions the contract to {@code
 * NOTICE_GIVEN}, generates and files the notice letter, and pushes the deposit's return-due
 * deadline out to the new effective end date.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContractTerminationService {

  private static final String ENTITY_TYPE = "CONTRACT";
  private static final int DEPOSIT_RETURN_GRACE_DAYS = 30;

  private final ContractRepository contractRepository;
  private final ContractTerminationRepository terminationRepository;
  private final TerminationRuleResolver ruleResolver;
  private final ContractService contractService;
  private final ContractTerminationLetterGenerator letterExporter;
  private final DepositService depositService;
  private final DocumentRepository documentRepository;
  private final S3StorageService s3StorageService;
  private final TransactionTemplate transactionTemplate;
  private final Clock clock;

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  @Transactional
  public ContractTerminationResponse terminate(
      ContractIdentifier identifier, TerminateContractRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Contract contract = contractRepository.getByIdentifierAndTeamId(identifier, teamId);

    if (terminationRepository.findByContractIdAndTeamId(contract.getId(), teamId).isPresent()) {
      throw new BusinessRuleException("This contract already has a termination on record");
    }

    var computation = ruleResolver.resolve(contract, request.givenBy(), request.noticeDate());
    LocalDate computedEndDate = request.noticeDate().plusDays(computation.noticeDays());
    LocalDate effectiveEndDate = request.effectiveEndDate().orElse(computedEndDate);

    Optional<String> groundCode = request.groundCode().map(String::strip).filter(s -> !s.isEmpty());
    if (computation.groundsRequired()) {
      if (groundCode.isEmpty()) {
        throw new BadRequestException("A groundCode is required to terminate this contract");
      }
      if (!computation.groundsCodes().isEmpty()
          && !computation.groundsCodes().contains(groundCode.get())) {
        throw new BadRequestException(
            "groundCode must be one of " + String.join(", ", computation.groundsCodes()));
      }
    }

    // A blank reason is no reason: Optional.of("   ") must not satisfy the override requirement.
    boolean hasOverrideReason =
        request.overrideReason().map(String::strip).filter(s -> !s.isEmpty()).isPresent();
    if (effectiveEndDate.isBefore(computedEndDate) && !hasOverrideReason) {
      throw new BadRequestException(
          "An effective end date earlier than the computed date requires an overrideReason");
    }

    ContractTermination termination =
        terminationRepository.save(
            ContractTermination.builder()
                .teamId(teamId)
                .contractId(contract.getId())
                .givenBy(request.givenBy())
                .noticeDate(request.noticeDate())
                .groundCode(groundCode)
                .computedEndDate(computedEndDate)
                .effectiveEndDate(effectiveEndDate)
                .overrideReason(request.overrideReason())
                .inspectionDate(request.inspectionDate())
                .status(ContractTerminationStatus.NOTICE_GIVEN)
                .createdBy(principal.getUserId())
                .updatedBy(principal.getUserId())
                .build());

    // Directly via the package-private transitionStatus: changeContractStatus deliberately refuses
    // NOTICE_GIVEN so the generic status endpoint cannot bypass this workflow. This method's own
    // @PreAuthorize is the authorization gate.
    contractService.transitionStatus(
        contract, Contract.ContractStatus.NOTICE_GIVEN, principal.getUserId(), Optional.empty());

    Document savedLetter =
        generateAndFileNoticeLetter(contract, termination, identifier, teamId, principal);
    termination.setNoticeLetterDocumentId(Optional.of(savedLetter.getId()));
    termination = terminationRepository.save(termination);

    depositService.updateReturnDueDate(
        identifier, effectiveEndDate.plusDays(DEPOSIT_RETURN_GRACE_DAYS), principal);

    log.info(
        "Contract {} terminated: notice given {}, effective end date {}",
        identifier.value(),
        request.noticeDate(),
        effectiveEndDate);

    return toResponse(termination, identifier, savedLetter.getIdentifier());
  }

  /**
   * Daily sweep: transitions every {@code NOTICE_GIVEN} termination whose effective end date has
   * passed to {@code TERMINATED}, along with its underlying contract. {@link
   * ContractTerminationRepository#findDueForTransition} only returns {@code NOTICE_GIVEN} rows, so
   * an already-{@code TERMINATED} termination is never picked up twice.
   *
   * <p>Runs with no authenticated team context (invoked from {@code ContractTerminationSweepJob}, a
   * Quartz job with no {@link UserPrincipal}), so the contract transition goes through {@link
   * ContractService#transitionStatus}, not the human-facing, {@code @PreAuthorize}-gated {@link
   * ContractService#changeContractStatus}.
   *
   * <p>Each termination is processed in its own {@link #transactionTemplate}-managed transaction —
   * deliberately NOT one {@code @Transactional} spanning the whole sweep — mirroring {@link
   * ContractExtensionService#processAutoExtensionsForTeam}. {@link
   * ContractService#transitionStatus} calls other {@code @Transactional} (REQUIRED-propagation)
   * services (payment scheduling, notifications); if one of those throws, it marks whatever ambient
   * transaction it joined {@code rollbackOnly}. A single method-level {@code @Transactional} here
   * would make that one ambient transaction span every termination in the run, so one failure would
   * roll back every termination already-processed earlier in the same sweep on commit (via {@code
   * UnexpectedRollbackException}) — even though the {@code catch} below appears to isolate it.
   * Giving each termination its own {@code transactionTemplate.executeWithoutResult} call gives
   * each one its own transaction, so a failure on one is caught, logged, and leaves every other
   * termination's already-committed work intact; the failed row stays {@code NOTICE_GIVEN} and is
   * picked up again on the next run.
   */
  public void sweepDueTerminations() {
    List<ContractTermination> due =
        terminationRepository.findDueForTransition(LocalDate.now(clock));
    for (ContractTermination termination : due) {
      try {
        transactionTemplate.executeWithoutResult(status -> sweepOneTermination(termination));
      } catch (Exception e) {
        log.error(
            "Failed to sweep termination {} for contract {} to TERMINATED",
            termination.getId(),
            termination.getContractId(),
            e);
      }
    }
  }

  private void sweepOneTermination(ContractTermination termination) {
    Contract contract =
        contractRepository.getByIdAndTeamId(termination.getContractId(), termination.getTeamId());

    termination.setStatus(ContractTerminationStatus.TERMINATED);
    terminationRepository.save(termination);

    // If some other path already moved the contract to TERMINATED, transitionStatus's own
    // validateStatusTransition rejects TERMINATED->TERMINATED with an IllegalArgumentException —
    // which, thrown inside this method's transaction, rolls back the termination.save() above
    // too. The row reverts to NOTICE_GIVEN, findDueForTransition picks it up again on the next
    // run, and the same failure repeats forever. Skipping the call when the contract is already
    // TERMINATED reconciles this termination's own row without re-attempting an invalid
    // transition.
    if (contract.getStatus() != Contract.ContractStatus.TERMINATED) {
      contractService.transitionStatus(
          contract, Contract.ContractStatus.TERMINATED, Constants.SYSTEM_USER_ID, Optional.empty());
    }

    log.info(
        "Termination for contract {} swept to TERMINATED (effective end date {})",
        termination.getContractId(),
        termination.getEffectiveEndDate());
  }

  /** A pure computation of the notice period — no persistence, safe for any team member to run. */
  public record TerminationPreview(
      LocalDate computedEndDate,
      int noticeDays,
      boolean groundsRequired,
      List<String> groundsCodes,
      TerminationRuleResolver.Source source) {}

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  @Transactional(readOnly = true)
  public TerminationPreview previewTermination(
      ContractIdentifier identifier,
      TerminationGivenBy givenBy,
      LocalDate noticeDate,
      UserPrincipal principal) {
    Contract contract =
        contractRepository.getByIdentifierAndTeamId(identifier, principal.requireTeamId());
    var computation = ruleResolver.resolve(contract, givenBy, noticeDate);
    LocalDate computedEndDate = noticeDate.plusDays(computation.noticeDays());
    return new TerminationPreview(
        computedEndDate,
        computation.noticeDays(),
        computation.groundsRequired(),
        computation.groundsCodes(),
        computation.source());
  }

  /** Generates the notice letter PDF and files it as a {@link Document} linked to the contract. */
  private Document generateAndFileNoticeLetter(
      Contract contract,
      ContractTermination termination,
      ContractIdentifier contractIdentifier,
      UUID teamId,
      UserPrincipal principal) {
    byte[] letterPdf = letterExporter.generate(contract, termination, teamId);

    String filename = "contract-termination-notice-" + contractIdentifier.value() + ".pdf";
    String title = "Contract Termination Notice - " + contractIdentifier.value();

    Sid contractSid = contract.getIdentifier().orElseThrow();
    String fileKey =
        s3StorageService.uploadFile(
            letterPdf,
            MediaType.APPLICATION_PDF_VALUE,
            Sid.of(principal.requireTeamIdentifier()),
            ENTITY_TYPE,
            contractSid,
            filename);

    Document document =
        Document.builder()
            .teamId(teamId)
            .entityType(ENTITY_TYPE)
            .entityId(contract.getId())
            .fileKey(fileKey)
            .fileName(filename)
            .fileSize((long) letterPdf.length)
            .mimeType(MediaType.APPLICATION_PDF_VALUE)
            .title(Optional.of(title))
            .notes(Optional.empty())
            .uploadedBy(principal.getUserId())
            .build();

    return documentRepository.save(document);
  }

  private ContractTerminationResponse toResponse(
      ContractTermination termination,
      ContractIdentifier contractIdentifier,
      Optional<Sid> noticeLetterDocumentIdentifier) {
    return new ContractTerminationResponse(
        termination.getIdentifier().orElseThrow(),
        contractIdentifier,
        termination.getGivenBy(),
        termination.getNoticeDate(),
        termination.getGroundCode(),
        termination.getComputedEndDate(),
        termination.getEffectiveEndDate(),
        termination.getOverrideReason(),
        termination.getInspectionDate(),
        noticeLetterDocumentIdentifier,
        termination.getStatus(),
        termination.getCreatedAt(),
        termination.getUpdatedAt());
  }
}
