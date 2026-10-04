package com.buurman.service.notification;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Contract;
import com.buurman.domain.Document;
import com.buurman.domain.NotificationType;
import com.buurman.domain.Sid;
import com.buurman.domain.SignatureRequest;
import com.buurman.domain.SignatureRequestStatus;
import com.buurman.domain.SignatureSigner;
import com.buurman.domain.SignatureSignerStatus;
import com.buurman.domain.Team;
import com.buurman.exception.ForbiddenException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.SignatureRequestRepository;
import com.buurman.repository.SignatureSignerRepository;
import com.buurman.repository.TeamRepository;
import com.buurman.service.S3StorageService;
import com.buurman.service.esignature.SignatureProviderClient;
import com.buurman.service.esignature.SignedDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignatureWebhookService {

  /** Forward-only guard, same shape as {@link WebhookService}'s STATUS_RANK. */
  private static final Map<SignatureSignerStatus, Integer> STATUS_RANK =
      Map.of(
          SignatureSignerStatus.PENDING, 0,
          SignatureSignerStatus.VIEWED, 1,
          SignatureSignerStatus.SIGNED, 2,
          SignatureSignerStatus.DECLINED, 2);

  private final SignatureRequestRepository signatureRequestRepository;
  private final SignatureSignerRepository signatureSignerRepository;
  private final SignatureProviderClient providerClient;
  private final DocumentRepository documentRepository;
  private final S3StorageService s3StorageService;
  private final TeamRepository teamRepository;
  private final ContractRepository contractRepository;
  private final NotificationService notificationService;
  private final AppProperties appProperties;
  private final ObjectMapper objectMapper;

  /**
   * Deliberately does NOT catch and swallow processing exceptions (beyond the one genuinely
   * non-retryable case, unparseable JSON, handled below). {@code completeRequest} downloads the
   * signed PDF and certificate from Documenso and uploads both to S3 — a transient failure there
   * used to be logged and discarded, leaving the request stuck PENDING/PARTIALLY_SIGNED forever
   * with no way to recover. Letting the exception propagate instead surfaces it through {@code
   * GlobalExceptionHandler}'s generic handler as a 500, which Documenso's webhook job retries (it
   * already does, up to its own max-retries) — turning a silent, permanent failure into an
   * automatic retry of exactly the kind this webhook needs.
   */
  public void processDocumensoEvent(String rawPayload, @Nullable String secretHeader) {
    if (!providerClient.isValidWebhookSecret(secretHeader)) {
      log.warn("Documenso webhook secret verification failed");
      throw new ForbiddenException("Documenso webhook secret verification failed");
    }

    JsonNode root;
    try {
      root = objectMapper.readTree(rawPayload);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      // Malformed JSON is never retryable — Documenso would send the exact same bytes again.
      // Log and accept (200) so it isn't retried forever; everything past this point, by
      // contrast, is left to propagate (see the class-level note below).
      log.warn("Documenso webhook had unparseable payload: {}", e.getMessage());
      return;
    }
    String event = root.path("event").asText();
    JsonNode payload = root.path("payload");
    String envelopeId = payload.path("envelopeId").asText();

    if (envelopeId.isEmpty()) {
      log.warn("Documenso webhook missing envelopeId");
      return;
    }

    Optional<SignatureRequest> maybeRequest =
        signatureRequestRepository.findByProviderAndProviderSubmissionId(
            SignatureProviderClient.PROVIDER_DOCUMENSO, envelopeId);
    if (maybeRequest.isEmpty()) {
      log.debug("Documenso webhook for unknown envelope {} — ignoring", envelopeId);
      return;
    }
    SignatureRequest request = maybeRequest.get();

    List<SignatureSigner> signers =
        signatureSignerRepository.findBySignatureRequestId(request.getId(), request.getTeamId());
    // Merge function on purpose: a duplicate provider_signer_id should not happen, but without
    // it Collectors.toMap throws IllegalStateException instead of processing the rest of the
    // signers.
    Map<String, SignatureSigner> byProviderSignerId =
        signers.stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    SignatureSigner::getProviderSignerId, s -> s, (first, duplicate) -> first));

    for (JsonNode recipient : payload.path("recipients")) {
      String recipientId = recipient.path("id").asText();
      SignatureSigner signer = byProviderSignerId.get(recipientId);
      if (signer == null) {
        continue;
      }
      SignatureSignerStatus newStatus = mapSignerStatus(recipient);
      if (isForwardProgress(signer.getStatus(), newStatus)) {
        Optional<java.time.Instant> signedAt =
            recipient.has("signedAt") && !recipient.path("signedAt").isNull()
                ? Optional.of(java.time.Instant.parse(recipient.path("signedAt").asText()))
                : Optional.empty();
        signatureSignerRepository.updateStatus(
            signer.getId(), signer.getTeamId(), newStatus, signedAt);
        signer.setStatus(newStatus);
      }
    }

    switch (event) {
      case "DOCUMENT_REJECTED" -> declineRequest(request, payload);
      case "DOCUMENT_CANCELLED" -> finalizeRequestStatus(request, SignatureRequestStatus.CANCELLED);
      case "DOCUMENT_COMPLETED" -> completeRequest(request, envelopeId);
      default -> updatePartialProgress(request, signers);
    }
  }

  private SignatureSignerStatus mapSignerStatus(JsonNode recipient) {
    String signingStatus = recipient.path("signingStatus").asText("");
    String readStatus = recipient.path("readStatus").asText("");
    if ("REJECTED".equals(signingStatus)) {
      return SignatureSignerStatus.DECLINED;
    }
    if ("SIGNED".equals(signingStatus)) {
      return SignatureSignerStatus.SIGNED;
    }
    if ("OPENED".equals(readStatus)) {
      return SignatureSignerStatus.VIEWED;
    }
    return SignatureSignerStatus.PENDING;
  }

  private boolean isForwardProgress(
      SignatureSignerStatus current, SignatureSignerStatus candidate) {
    return STATUS_RANK.getOrDefault(candidate, 0) > STATUS_RANK.getOrDefault(current, 0);
  }

  private void updatePartialProgress(SignatureRequest request, List<SignatureSigner> signers) {
    boolean anySigned =
        signers.stream().anyMatch(s -> s.getStatus() == SignatureSignerStatus.SIGNED);
    if (anySigned && request.getStatus() == SignatureRequestStatus.PENDING) {
      finalizeRequestStatus(request, SignatureRequestStatus.PARTIALLY_SIGNED);
    }
  }

  private boolean isTerminal(SignatureRequestStatus status) {
    return status == SignatureRequestStatus.COMPLETED
        || status == SignatureRequestStatus.DECLINED
        || status == SignatureRequestStatus.CANCELLED;
  }

  private void finalizeRequestStatus(SignatureRequest request, SignatureRequestStatus status) {
    if (isTerminal(request.getStatus())) {
      return; // already terminal — never regress a terminal outcome
    }
    request.setStatus(status);
    signatureRequestRepository.save(request);
  }

  /**
   * A signer declined: the whole request is DECLINED and the landlord's team is told, because
   * nothing else will ever tell them — the tenant abandoned the flow on the provider's page.
   *
   * <p>The notify call sits inside the terminal guard on purpose. A duplicate or late
   * DOCUMENT_REJECTED for an already-DECLINED request must not email the team a second time.
   */
  private void declineRequest(SignatureRequest request, JsonNode payload) {
    if (isTerminal(request.getStatus())) {
      return; // already terminal — never regress the outcome, and never re-notify
    }
    request.setStatus(SignatureRequestStatus.DECLINED);
    signatureRequestRepository.save(request);
    notifyTeamOfDecline(request, payload);
  }

  private void notifyTeamOfDecline(SignatureRequest request, JsonNode payload) {
    Optional<Document> document =
        documentRepository.findByIdAndTeamId(request.getDocumentId(), request.getTeamId());
    String baseUrl = appProperties.email().baseUrl();
    String contractUrl =
        document
            .filter(d -> "CONTRACT".equals(d.getEntityType()))
            .flatMap(
                d -> contractRepository.findByIdAndTeamId(d.getEntityId(), request.getTeamId()))
            .flatMap(Contract::getIdentifier)
            .map(identifier -> baseUrl + "/contracts/" + identifier.value() + "?tab=documents")
            .orElse(baseUrl + "/contracts");

    notificationService.sendToTeam(
        SendNotificationRequest.builder()
            .teamId(Optional.of(request.getTeamId()))
            .notificationType(NotificationType.SIGNATURE_REQUEST_DECLINED)
            .templateName("signature-declined")
            .templateVariables(
                Map.of(
                    "documentName",
                    document.map(Document::getFileName).orElse("a document"),
                    "signerEmail",
                    declinedSignerEmail(payload).orElse("a signer"),
                    "baseUrl",
                    baseUrl,
                    "primaryUrl",
                    contractUrl))
            .createdBy(request.getUpdatedBy())
            .build());
  }

  /** The email of the recipient whose rejection triggered this event, if the payload names one. */
  private Optional<String> declinedSignerEmail(JsonNode payload) {
    for (JsonNode recipient : payload.path("recipients")) {
      if ("REJECTED".equals(recipient.path("signingStatus").asText(""))) {
        String email = recipient.path("email").asText("");
        if (!email.isEmpty()) {
          return Optional.of(email);
        }
      }
    }
    return Optional.empty();
  }

  private void completeRequest(SignatureRequest request, String envelopeId) {
    if (isTerminal(request.getStatus())) {
      // Duplicate DOCUMENT_COMPLETED, or a late/out-of-order delivery arriving after the request
      // was already DECLINED/CANCELLED — do not re-download/re-upload or overwrite the outcome.
      return;
    }

    SignedDocument signed = providerClient.downloadCompleted(envelopeId);
    Document original =
        documentRepository
            .findByIdAndTeamId(request.getDocumentId(), request.getTeamId())
            .orElseThrow();
    Sid teamIdentifier =
        teamRepository.findById(request.getTeamId()).flatMap(Team::getIdentifier).orElseThrow();
    Sid documentIdentifier = original.getIdentifier().orElseThrow();

    // A retry (e.g. the webhook redelivering after this method threw partway through on a prior
    // attempt, before the request reached COMPLETED) must not re-upload a signed PDF that's
    // already there — reuse it instead of creating a duplicate Document/S3 object.
    String fileName = "signed-" + original.getFileName();
    Document savedSignedDocument =
        documentRepository
            .findByEntityAndFileNamePatternAndTeamId(
                original.getEntityType(), original.getEntityId(), fileName, request.getTeamId())
            .stream()
            .findFirst()
            .orElseGet(
                () ->
                    uploadSignedDocument(
                        request, original, teamIdentifier, documentIdentifier, signed, fileName));

    // The provider's audit certificate is the evidence of who signed what and when, so it is
    // persisted as its own Document on the same entity as the signed PDF — sourceDocumentId
    // groups it (and the signed copy) under the original in the Documents list. It deliberately
    // does not go in signature_requests.signed_document_id: that column points at the signed PDF
    // only.
    Document savedCertificateDocument =
        persistCertificate(request, original, teamIdentifier, documentIdentifier, signed);

    request.setSignedDocumentId(Optional.of(savedSignedDocument.getId()));
    request.setStatus(SignatureRequestStatus.COMPLETED);
    signatureRequestRepository.save(request);

    log.info(
        "Signature request {} completed; signed document {} and certificate {} stored",
        envelopeId,
        savedSignedDocument.getId(),
        savedCertificateDocument.getId());
  }

  private Document uploadSignedDocument(
      SignatureRequest request,
      Document original,
      Sid teamIdentifier,
      Sid documentIdentifier,
      SignedDocument signed,
      String fileName) {
    String fileKey =
        s3StorageService.uploadFile(
            signed.signedPdfBytes(),
            MediaType.APPLICATION_PDF_VALUE,
            teamIdentifier,
            original.getEntityType(),
            documentIdentifier,
            fileName);

    Document signedDocument =
        Document.builder()
            .teamId(request.getTeamId())
            .entityType(original.getEntityType())
            .entityId(original.getEntityId())
            .fileKey(fileKey)
            .fileName(fileName)
            .fileSize((long) signed.signedPdfBytes().length)
            .mimeType(MediaType.APPLICATION_PDF_VALUE)
            .title(Optional.of("Signed: " + original.getFileName()))
            .notes(Optional.empty())
            .sourceDocumentId(Optional.of(original.getId()))
            .uploadedBy(request.getUpdatedBy())
            .build();
    return documentRepository.save(signedDocument);
  }

  private Document persistCertificate(
      SignatureRequest request,
      Document original,
      Sid teamIdentifier,
      Sid documentIdentifier,
      SignedDocument signed) {
    String certificateFileName = "certificate-" + original.getFileName();

    // Same retry hazard as the signed PDF above: a redelivery landing after this upload
    // succeeded but before the request reached COMPLETED must reuse it, not duplicate it.
    return documentRepository
        .findByEntityAndFileNamePatternAndTeamId(
            original.getEntityType(),
            original.getEntityId(),
            certificateFileName,
            request.getTeamId())
        .stream()
        .findFirst()
        .orElseGet(
            () ->
                uploadCertificate(
                    request,
                    original,
                    teamIdentifier,
                    documentIdentifier,
                    signed,
                    certificateFileName));
  }

  private Document uploadCertificate(
      SignatureRequest request,
      Document original,
      Sid teamIdentifier,
      Sid documentIdentifier,
      SignedDocument signed,
      String certificateFileName) {
    String certificateFileKey =
        s3StorageService.uploadFile(
            signed.certificatePdfBytes(),
            MediaType.APPLICATION_PDF_VALUE,
            teamIdentifier,
            original.getEntityType(),
            documentIdentifier,
            certificateFileName);

    return documentRepository.save(
        Document.builder()
            .teamId(request.getTeamId())
            .entityType(original.getEntityType())
            .entityId(original.getEntityId())
            .fileKey(certificateFileKey)
            .fileName(certificateFileName)
            .fileSize((long) signed.certificatePdfBytes().length)
            .mimeType(MediaType.APPLICATION_PDF_VALUE)
            .title(Optional.of("Signing certificate: " + original.getFileName()))
            .notes(Optional.empty())
            .sourceDocumentId(Optional.of(original.getId()))
            .uploadedBy(request.getUpdatedBy())
            .build());
  }
}
