package com.buurman.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractTermination;
import com.buurman.domain.ContractTerminationStatus;
import com.buurman.domain.Document;
import com.buurman.domain.Sid;
import com.buurman.domain.TerminationGivenBy;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.dto.request.ChangeContractStatusRequest;
import com.buurman.dto.request.TerminateContractRequest;
import com.buurman.dto.response.ContractTerminationResponse;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.ContractTerminationRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.regulation.TerminationRuleResolver;

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

    if (effectiveEndDate.isBefore(computedEndDate) && request.overrideReason().isEmpty()) {
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
                .groundCode(request.groundCode())
                .computedEndDate(computedEndDate)
                .effectiveEndDate(effectiveEndDate)
                .overrideReason(request.overrideReason())
                .inspectionDate(request.inspectionDate())
                .status(ContractTerminationStatus.NOTICE_GIVEN)
                .createdBy(principal.getUserId())
                .updatedBy(principal.getUserId())
                .build());

    contractService.changeContractStatus(
        identifier,
        new ChangeContractStatusRequest(Contract.ContractStatus.NOTICE_GIVEN, Optional.empty()),
        principal);

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
