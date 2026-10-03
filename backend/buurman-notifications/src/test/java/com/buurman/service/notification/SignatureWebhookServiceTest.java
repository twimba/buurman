package com.buurman.service.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.config.models.AppProperties;
import com.buurman.domain.Document;
import com.buurman.domain.NotificationType;
import com.buurman.domain.SignatureRequest;
import com.buurman.domain.SignatureRequestStatus;
import com.buurman.domain.SignatureSigner;
import com.buurman.domain.SignatureSignerRole;
import com.buurman.domain.SignatureSignerStatus;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.SignatureRequestRepository;
import com.buurman.repository.SignatureSignerRepository;
import com.buurman.service.S3StorageService;
import com.buurman.service.esignature.SignatureProviderClient;
import com.buurman.service.esignature.SignedDocument;
import com.fasterxml.jackson.databind.ObjectMapper;

class SignatureWebhookServiceTest {

  private final SignatureRequestRepository signatureRequestRepository =
      mock(SignatureRequestRepository.class);
  private final SignatureSignerRepository signatureSignerRepository =
      mock(SignatureSignerRepository.class);
  private final SignatureProviderClient providerClient = mock(SignatureProviderClient.class);
  private final DocumentRepository documentRepository = mock(DocumentRepository.class);
  private final S3StorageService s3StorageService = mock(S3StorageService.class);
  private final com.buurman.repository.TeamRepository teamRepository =
      mock(com.buurman.repository.TeamRepository.class);
  private final com.buurman.repository.ContractRepository contractRepository =
      mock(com.buurman.repository.ContractRepository.class);
  private final NotificationService notificationService = mock(NotificationService.class);
  private final AppProperties appProperties =
      new AppProperties(
          "test",
          "https://app.test",
          new AppProperties.Email("no-reply@test", "Buurman", "https://app.test"),
          new AppProperties.Api("https://api.test"),
          new AppProperties.Cors(List.of(), List.of()),
          new AppProperties.Documents(1024L, List.of("application/pdf")));

  private SignatureWebhookService service;

  private static final UUID REQUEST_ID = UUID.randomUUID();
  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID DOCUMENT_ID = UUID.randomUUID();
  private static final UUID SIGNER_ID = UUID.randomUUID();
  private static final UUID UPDATED_BY = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new SignatureWebhookService(
            signatureRequestRepository,
            signatureSignerRepository,
            providerClient,
            documentRepository,
            s3StorageService,
            teamRepository,
            contractRepository,
            notificationService,
            appProperties,
            new ObjectMapper());
    when(providerClient.isValidWebhookSecret(any())).thenReturn(true);
    when(teamRepository.findById(TEAM_ID))
        .thenReturn(
            Optional.of(
                com.buurman.domain.Team.builder()
                    .id(TEAM_ID)
                    .identifier(
                        Optional.of(com.buurman.domain.Sid.of("TEA00000000000000000000001")))
                    .build()));
  }

  private SignatureRequest existingRequest(SignatureRequestStatus status) {
    return SignatureRequest.builder()
        .id(REQUEST_ID)
        .teamId(TEAM_ID)
        .documentId(DOCUMENT_ID)
        .provider("documenso")
        .providerSubmissionId("envelope_abc123")
        .status(status)
        .updatedBy(UPDATED_BY)
        .build();
  }

  private SignatureSigner existingSigner(SignatureSignerStatus status) {
    return SignatureSigner.builder()
        .id(SIGNER_ID)
        .signatureRequestId(REQUEST_ID)
        .email("tenant@example.com")
        .role(SignatureSignerRole.TENANT)
        .providerSignerId("52")
        .status(status)
        .build();
  }

  @Test
  @DisplayName("invalid webhook secret is rejected before any DB read")
  void invalidSecretRejected() {
    when(providerClient.isValidWebhookSecret("wrong")).thenReturn(false);

    assertThatThrownByProcessing(openedEventPayload(), "wrong");

    verify(signatureRequestRepository, never()).findByProviderAndProviderSubmissionId(any(), any());
  }

  @Test
  @DisplayName(
      "unknown envelope id (no matching signature_request) is ignored safely, never throws")
  void unknownEnvelopeIdIgnoredSafely() {
    when(signatureRequestRepository.findByProviderAndProviderSubmissionId(
            "documenso", "envelope_abc123"))
        .thenReturn(Optional.empty());

    service.processDocumensoEvent(openedEventPayload(), "secret");

    verify(signatureSignerRepository, never()).findBySignatureRequestId(any());
  }

  @Test
  @DisplayName("DOCUMENT_OPENED sets a PENDING signer to VIEWED")
  void documentOpenedSetsViewed() {
    when(signatureRequestRepository.findByProviderAndProviderSubmissionId(
            "documenso", "envelope_abc123"))
        .thenReturn(Optional.of(existingRequest(SignatureRequestStatus.PENDING)));
    when(signatureSignerRepository.findBySignatureRequestId(REQUEST_ID))
        .thenReturn(List.of(existingSigner(SignatureSignerStatus.PENDING)));

    service.processDocumensoEvent(openedEventPayload(), "secret");

    verify(signatureSignerRepository)
        .updateStatus(SIGNER_ID, SignatureSignerStatus.VIEWED, Optional.empty());
  }

  @Test
  @DisplayName("a stale DOCUMENT_OPENED arriving after SIGNED does not regress the signer's status")
  void staleOpenedDoesNotRegressSignedSigner() {
    when(signatureRequestRepository.findByProviderAndProviderSubmissionId(
            "documenso", "envelope_abc123"))
        .thenReturn(Optional.of(existingRequest(SignatureRequestStatus.PARTIALLY_SIGNED)));
    when(signatureSignerRepository.findBySignatureRequestId(REQUEST_ID))
        .thenReturn(List.of(existingSigner(SignatureSignerStatus.SIGNED)));

    service.processDocumensoEvent(openedEventPayload(), "secret");

    verify(signatureSignerRepository, never()).updateStatus(eq(SIGNER_ID), any(), any());
  }

  @Test
  @DisplayName("DOCUMENT_REJECTED sets the signer DECLINED and the request DECLINED")
  void documentRejectedDeclines() {
    when(signatureRequestRepository.findByProviderAndProviderSubmissionId(
            "documenso", "envelope_abc123"))
        .thenReturn(Optional.of(existingRequest(SignatureRequestStatus.PENDING)));
    when(signatureSignerRepository.findBySignatureRequestId(REQUEST_ID))
        .thenReturn(List.of(existingSigner(SignatureSignerStatus.PENDING)));

    service.processDocumensoEvent(rejectedEventPayload(), "secret");

    verify(signatureSignerRepository)
        .updateStatus(SIGNER_ID, SignatureSignerStatus.DECLINED, Optional.empty());
    verify(signatureRequestRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                r -> r.getStatus() == SignatureRequestStatus.DECLINED));
  }

  @Test
  @DisplayName(
      "a decline notifies the landlord's team exactly once, naming the document and signer")
  void declineNotifiesTheTeamOnce() {
    when(signatureRequestRepository.findByProviderAndProviderSubmissionId(
            "documenso", "envelope_abc123"))
        .thenReturn(Optional.of(existingRequest(SignatureRequestStatus.PENDING)));
    when(signatureSignerRepository.findBySignatureRequestId(REQUEST_ID))
        .thenReturn(List.of(existingSigner(SignatureSignerStatus.PENDING)));
    UUID contractId = UUID.randomUUID();
    when(documentRepository.findByIdAndTeamId(DOCUMENT_ID, TEAM_ID))
        .thenReturn(
            Optional.of(
                Document.builder()
                    .id(DOCUMENT_ID)
                    .teamId(TEAM_ID)
                    .entityType("CONTRACT")
                    .entityId(contractId)
                    .fileName("addendum.pdf")
                    .build()));
    when(contractRepository.findByIdAndTeamId(contractId, TEAM_ID))
        .thenReturn(
            Optional.of(
                com.buurman.domain.Contract.builder()
                    .id(contractId)
                    .identifier(
                        Optional.of(com.buurman.domain.Sid.of("CON00000000000000000000001")))
                    .build()));

    service.processDocumensoEvent(rejectedEventPayload(), "secret");

    verify(notificationService)
        .sendToTeam(
            org.mockito.ArgumentMatchers.argThat(
                sent ->
                    sent.notificationType() == NotificationType.SIGNATURE_REQUEST_DECLINED
                        && sent.teamId().equals(Optional.of(TEAM_ID))
                        && "signature-declined".equals(sent.templateName())
                        && "addendum.pdf".equals(sent.templateVariables().get("documentName"))
                        && "tenant@example.com"
                            .equals(sent.templateVariables().get("signerEmail"))));
    verify(notificationService, org.mockito.Mockito.times(1)).sendToTeam(any());
  }

  @Test
  @DisplayName("a repeat DOCUMENT_REJECTED for an already-DECLINED request does not re-notify")
  void repeatDeclineDoesNotReNotify() {
    when(signatureRequestRepository.findByProviderAndProviderSubmissionId(
            "documenso", "envelope_abc123"))
        .thenReturn(Optional.of(existingRequest(SignatureRequestStatus.DECLINED)));
    when(signatureSignerRepository.findBySignatureRequestId(REQUEST_ID))
        .thenReturn(List.of(existingSigner(SignatureSignerStatus.DECLINED)));

    service.processDocumensoEvent(rejectedEventPayload(), "secret");

    verify(notificationService, never()).sendToTeam(any());
    verify(signatureRequestRepository, never()).save(any());
  }

  @Test
  @DisplayName(
      "DOCUMENT_COMPLETED downloads the signed PDF+certificate, uploads, and marks COMPLETED")
  void documentCompletedDownloadsAndPersists() {
    when(signatureRequestRepository.findByProviderAndProviderSubmissionId(
            "documenso", "envelope_abc123"))
        .thenReturn(Optional.of(existingRequest(SignatureRequestStatus.PARTIALLY_SIGNED)));
    when(signatureSignerRepository.findBySignatureRequestId(REQUEST_ID))
        .thenReturn(List.of(existingSigner(SignatureSignerStatus.SIGNED)));
    when(providerClient.downloadCompleted("envelope_abc123"))
        .thenReturn(new SignedDocument("%PDF-signed".getBytes(), "%PDF-cert".getBytes()));
    Document originalDocument =
        Document.builder()
            .id(DOCUMENT_ID)
            .teamId(TEAM_ID)
            .entityType("CONTRACT")
            .entityId(UUID.randomUUID())
            .identifier(Optional.of(com.buurman.domain.Sid.of("DOC00000000000000000000009")))
            .fileName("addendum.pdf")
            .build();
    when(documentRepository.findByIdAndTeamId(DOCUMENT_ID, TEAM_ID))
        .thenReturn(Optional.of(originalDocument));
    when(s3StorageService.uploadFile(any(byte[].class), any(), any(), any(), any(), any()))
        .thenReturn("k2");
    when(documentRepository.save(any()))
        .thenAnswer(
            invocation -> {
              Document d = invocation.getArgument(0);
              d.setId(UUID.randomUUID());
              return d;
            });

    service.processDocumensoEvent(completedEventPayload(), "secret");

    verify(signatureRequestRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                r -> r.getStatus() == SignatureRequestStatus.COMPLETED));

    // Both the signed PDF and the provider's audit certificate are persisted as Documents on the
    // same entity, so the certificate is discoverable in the contract's normal Documents list.
    org.mockito.ArgumentCaptor<byte[]> uploadedBytes =
        org.mockito.ArgumentCaptor.forClass(byte[].class);
    org.mockito.ArgumentCaptor<String> uploadedNames =
        org.mockito.ArgumentCaptor.forClass(String.class);
    verify(s3StorageService, org.mockito.Mockito.times(2))
        .uploadFile(uploadedBytes.capture(), any(), any(), any(), any(), uploadedNames.capture());
    org.assertj.core.api.Assertions.assertThat(uploadedNames.getAllValues())
        .containsExactly("signed-addendum.pdf", "certificate-addendum.pdf");
    org.assertj.core.api.Assertions.assertThat(uploadedBytes.getAllValues())
        .containsExactly("%PDF-signed".getBytes(), "%PDF-cert".getBytes());

    org.mockito.ArgumentCaptor<Document> savedDocuments =
        org.mockito.ArgumentCaptor.forClass(Document.class);
    verify(documentRepository, org.mockito.Mockito.times(2)).save(savedDocuments.capture());
    org.assertj.core.api.Assertions.assertThat(savedDocuments.getAllValues())
        .extracting(Document::getFileName, d -> d.getTitle().orElse(""))
        .containsExactly(
            org.assertj.core.api.Assertions.tuple("signed-addendum.pdf", "Signed: addendum.pdf"),
            org.assertj.core.api.Assertions.tuple(
                "certificate-addendum.pdf", "Signing certificate: addendum.pdf"));
    org.assertj.core.api.Assertions.assertThat(savedDocuments.getAllValues())
        .allSatisfy(
            d -> {
              org.assertj.core.api.Assertions.assertThat(d.getEntityType()).isEqualTo("CONTRACT");
              org.assertj.core.api.Assertions.assertThat(d.getEntityId())
                  .isEqualTo(originalDocument.getEntityId());
            });
  }

  @Test
  @DisplayName(
      "a redelivered DOCUMENT_COMPLETED reuses an already-uploaded signed PDF/certificate instead"
          + " of duplicating them")
  void documentCompletedRetryReusesAlreadyUploadedDocuments() {
    when(signatureRequestRepository.findByProviderAndProviderSubmissionId(
            "documenso", "envelope_abc123"))
        .thenReturn(Optional.of(existingRequest(SignatureRequestStatus.PARTIALLY_SIGNED)));
    when(signatureSignerRepository.findBySignatureRequestId(REQUEST_ID))
        .thenReturn(List.of(existingSigner(SignatureSignerStatus.SIGNED)));
    when(providerClient.downloadCompleted("envelope_abc123"))
        .thenReturn(new SignedDocument("%PDF-signed".getBytes(), "%PDF-cert".getBytes()));
    Document originalDocument =
        Document.builder()
            .id(DOCUMENT_ID)
            .teamId(TEAM_ID)
            .entityType("CONTRACT")
            .entityId(UUID.randomUUID())
            .identifier(Optional.of(com.buurman.domain.Sid.of("DOC00000000000000000000009")))
            .fileName("addendum.pdf")
            .build();
    when(documentRepository.findByIdAndTeamId(DOCUMENT_ID, TEAM_ID))
        .thenReturn(Optional.of(originalDocument));

    // A prior, partially-failed attempt already uploaded the signed PDF — this is what a
    // webhook redelivery after that partial success looks like from the repository's view.
    Document alreadyUploadedSigned =
        Document.builder()
            .id(UUID.randomUUID())
            .teamId(TEAM_ID)
            .entityType("CONTRACT")
            .entityId(originalDocument.getEntityId())
            .fileName("signed-addendum.pdf")
            .build();
    when(documentRepository.findByEntityAndFileNamePatternAndTeamId(
            "CONTRACT", originalDocument.getEntityId(), "signed-addendum.pdf", TEAM_ID))
        .thenReturn(List.of(alreadyUploadedSigned));
    when(s3StorageService.uploadFile(any(byte[].class), any(), any(), any(), any(), any()))
        .thenReturn("k2");
    when(documentRepository.save(any()))
        .thenAnswer(
            invocation -> {
              Document d = invocation.getArgument(0);
              d.setId(UUID.randomUUID());
              return d;
            });

    service.processDocumensoEvent(completedEventPayload(), "secret");

    // Only the certificate gets uploaded/saved this time; the signed PDF is reused as-is.
    verify(s3StorageService, org.mockito.Mockito.times(1))
        .uploadFile(any(), any(), any(), any(), any(), eq("certificate-addendum.pdf"));
    org.mockito.ArgumentCaptor<Document> savedDocuments =
        org.mockito.ArgumentCaptor.forClass(Document.class);
    verify(documentRepository, org.mockito.Mockito.times(1)).save(savedDocuments.capture());
    org.assertj.core.api.Assertions.assertThat(savedDocuments.getValue().getFileName())
        .isEqualTo("certificate-addendum.pdf");
    verify(signatureRequestRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                r ->
                    r.getStatus() == SignatureRequestStatus.COMPLETED
                        && r.getSignedDocumentId().orElse(null) == alreadyUploadedSigned.getId()));
  }

  @Test
  @DisplayName(
      "a processing failure during DOCUMENT_COMPLETED propagates instead of being swallowed, so"
          + " the webhook responds 5xx and Documenso retries delivery")
  void processingFailurePropagates() {
    when(signatureRequestRepository.findByProviderAndProviderSubmissionId(
            "documenso", "envelope_abc123"))
        .thenReturn(Optional.of(existingRequest(SignatureRequestStatus.PARTIALLY_SIGNED)));
    when(signatureSignerRepository.findBySignatureRequestId(REQUEST_ID))
        .thenReturn(List.of(existingSigner(SignatureSignerStatus.SIGNED)));
    when(providerClient.downloadCompleted("envelope_abc123"))
        .thenThrow(new com.buurman.exception.ExternalServiceException("Documenso unreachable"));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.processDocumensoEvent(completedEventPayload(), "secret"))
        .isInstanceOf(com.buurman.exception.ExternalServiceException.class);

    verify(signatureRequestRepository, never())
        .save(
            org.mockito.ArgumentMatchers.argThat(
                r -> r.getStatus() == SignatureRequestStatus.COMPLETED));
  }

  @Test
  @DisplayName(
      "a stale DOCUMENT_COMPLETED arriving after DECLINED does not re-download or overwrite the"
          + " decline")
  void staleCompletedDoesNotRegressDeclinedRequest() {
    when(signatureRequestRepository.findByProviderAndProviderSubmissionId(
            "documenso", "envelope_abc123"))
        .thenReturn(Optional.of(existingRequest(SignatureRequestStatus.DECLINED)));
    when(signatureSignerRepository.findBySignatureRequestId(REQUEST_ID))
        .thenReturn(List.of(existingSigner(SignatureSignerStatus.DECLINED)));

    service.processDocumensoEvent(completedEventPayload(), "secret");

    verify(providerClient, never()).downloadCompleted(any());
    verify(signatureRequestRepository, never())
        .save(
            org.mockito.ArgumentMatchers.argThat(
                r -> r.getStatus() == SignatureRequestStatus.COMPLETED));
  }

  @Test
  @DisplayName(
      "a duplicate DOCUMENT_COMPLETED for an already-COMPLETED request does not re-download or"
          + " re-save")
  void duplicateCompletedEventOnAlreadyCompletedRequestIsANoop() {
    when(signatureRequestRepository.findByProviderAndProviderSubmissionId(
            "documenso", "envelope_abc123"))
        .thenReturn(Optional.of(existingRequest(SignatureRequestStatus.COMPLETED)));
    when(signatureSignerRepository.findBySignatureRequestId(REQUEST_ID))
        .thenReturn(List.of(existingSigner(SignatureSignerStatus.SIGNED)));

    service.processDocumensoEvent(completedEventPayload(), "secret");

    verify(providerClient, never()).downloadCompleted(any());
    verify(documentRepository, never()).save(any());
    verify(signatureRequestRepository, never()).save(any());
  }

  private void assertThatThrownByProcessing(String payload, String secret) {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.processDocumensoEvent(payload, secret))
        .isInstanceOf(com.buurman.exception.ForbiddenException.class);
  }

  private String openedEventPayload() {
    return """
    {"event":"DOCUMENT_OPENED","payload":{"envelopeId":"envelope_abc123","status":"PENDING",
    "recipients":[{"id":52,"email":"tenant@example.com","readStatus":"OPENED","signingStatus":"NOT_SIGNED"}]}}
    """;
  }

  private String rejectedEventPayload() {
    return """
    {"event":"DOCUMENT_REJECTED","payload":{"envelopeId":"envelope_abc123","status":"REJECTED",
    "recipients":[{"id":52,"email":"tenant@example.com","signingStatus":"REJECTED"}]}}
    """;
  }

  private String completedEventPayload() {
    return """
    {"event":"DOCUMENT_COMPLETED","payload":{"envelopeId":"envelope_abc123","status":"COMPLETED",
    "recipients":[{"id":52,"email":"tenant@example.com","signingStatus":"SIGNED"}]}}
    """;
  }
}
