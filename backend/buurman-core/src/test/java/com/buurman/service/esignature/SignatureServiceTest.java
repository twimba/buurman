package com.buurman.service.esignature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.Contact;
import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Document;
import com.buurman.domain.Sid;
import com.buurman.domain.SignatureRequestStatus;
import com.buurman.domain.SignatureSigner;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.SignatureRequestIdentifier;
import com.buurman.exception.BusinessRuleException;
import com.buurman.exception.ExternalServiceException;
import com.buurman.exception.NotFoundException;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractPartyRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.SignatureRequestRepository;
import com.buurman.repository.SignatureSignerRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.S3StorageService;

class SignatureServiceTest {

  private final FeatureFlagService featureFlagService = mock(FeatureFlagService.class);
  private final DocumentRepository documentRepository = mock(DocumentRepository.class);
  private final ContractRepository contractRepository = mock(ContractRepository.class);
  private final ContractPartyRepository contractPartyRepository =
      mock(ContractPartyRepository.class);
  private final ContactRepository contactRepository = mock(ContactRepository.class);
  private final S3StorageService s3StorageService = mock(S3StorageService.class);
  private final SignatureProviderClient providerClient = mock(SignatureProviderClient.class);
  private final SignatureRequestRepository signatureRequestRepository =
      mock(SignatureRequestRepository.class);
  private final SignatureSignerRepository signatureSignerRepository =
      mock(SignatureSignerRepository.class);

  private SignatureService service;

  private static final UUID TEAM_ID = UUID.randomUUID();
  private static final UUID CONTRACT_ID = UUID.randomUUID();
  private static final UUID DOCUMENT_ID = UUID.randomUUID();
  private static final UUID CONTACT_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();

  private final UserPrincipal principal =
      new UserPrincipal(
          USER_ID, "USR1", "kc-1", "landlord@example.com", "Landlord", TEAM_ID, "TEA1", null);

  @BeforeEach
  void setUp() {
    service =
        new SignatureService(
            featureFlagService,
            documentRepository,
            contractRepository,
            contractPartyRepository,
            contactRepository,
            s3StorageService,
            providerClient,
            signatureRequestRepository,
            signatureSignerRepository);

    when(featureFlagService.isEnabled("esignature_enabled", principal)).thenReturn(true);

    Document document =
        Document.builder()
            .id(DOCUMENT_ID)
            .teamId(TEAM_ID)
            .fileKey("k")
            .fileName("addendum.pdf")
            .build();
    when(documentRepository.getByIdentifierAndTeamId(
            any(Sid.class), org.mockito.ArgumentMatchers.eq(TEAM_ID)))
        .thenReturn(document);
    when(s3StorageService.downloadFile("k"))
        .thenReturn(new ByteArrayInputStream("%PDF-1.7\ndoc".getBytes()));

    com.buurman.domain.Contract contract =
        com.buurman.domain.Contract.builder().id(CONTRACT_ID).build();
    when(contractRepository.getByIdentifierAndTeamId(
            any(Sid.class), org.mockito.ArgumentMatchers.eq(TEAM_ID)))
        .thenReturn(contract);

    ContractParty tenantParty =
        ContractParty.builder()
            .contractId(CONTRACT_ID)
            .contactId(Optional.of(CONTACT_ID))
            .role(ContractPartyRole.PRIMARY_TENANT)
            .build();
    when(contractPartyRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of(tenantParty));

    Contact tenantContact =
        Contact.builder().id(CONTACT_ID).email(Optional.of("tenant@example.com")).build();
    when(contactRepository.findByIdAndTeamId(CONTACT_ID, TEAM_ID))
        .thenReturn(Optional.of(tenantContact));
  }

  @Test
  @DisplayName(
      "rejects when the feature flag is disabled, before touching the provider or the database")
  void rejectsWhenFlagDisabled() {
    when(featureFlagService.isEnabled("esignature_enabled", principal)).thenReturn(false);

    assertThatThrownBy(
            () ->
                service.createSignatureRequest(
                    ContractIdentifier.of("CON00000000000000000000001"),
                    DocumentIdentifier.of("DOC00000000000000000000001"),
                    principal))
        .isInstanceOf(BusinessRuleException.class);

    verifyNoInteractions(providerClient, signatureRequestRepository, signatureSignerRepository);
  }

  @Test
  @DisplayName("a document identifier from another team is not found — no cross-team leak")
  void crossTeamDocumentIsNotFound() {
    when(documentRepository.getByIdentifierAndTeamId(
            any(Sid.class), org.mockito.ArgumentMatchers.eq(TEAM_ID)))
        .thenThrow(new NotFoundException("Document not found"));

    assertThatThrownBy(
            () ->
                service.createSignatureRequest(
                    ContractIdentifier.of("CON00000000000000000000001"),
                    DocumentIdentifier.of("DOC00000000000000000000001"),
                    principal))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName(
      "sends the document to landlord (principal) + every tenant contract party with an email")
  void sendsToLandlordAndTenants() {
    when(providerClient.createSubmission(any(byte[].class), any(String.class), anyList()))
        .thenReturn(
            new SignatureSubmission(
                "envelope_1",
                List.of(
                    new ProviderSigner("1", "landlord@example.com"),
                    new ProviderSigner("2", "tenant@example.com"))));
    when(signatureRequestRepository.save(any()))
        .thenAnswer(
            invocation -> {
              var request = (com.buurman.domain.SignatureRequest) invocation.getArgument(0);
              request.setId(UUID.randomUUID());
              request.setIdentifier(Optional.of(Sid.of("SGR00000000000000000000001")));
              return request;
            });
    when(signatureSignerRepository.save(any()))
        .thenAnswer(
            invocation -> {
              var signer = (SignatureSigner) invocation.getArgument(0);
              signer.setId(UUID.randomUUID());
              return signer;
            });

    var response =
        service.createSignatureRequest(
            ContractIdentifier.of("CON00000000000000000000001"),
            DocumentIdentifier.of("DOC00000000000000000000001"),
            principal);

    assertThat(response.status()).isEqualTo(SignatureRequestStatus.PENDING);
    org.mockito.Mockito.verify(providerClient)
        .createSubmission(
            any(byte[].class),
            any(String.class),
            org.mockito.ArgumentMatchers.argThat(
                signers ->
                    signers.size() == 2
                        && signers.stream().anyMatch(s -> s.email().equals("landlord@example.com"))
                        && signers.stream().anyMatch(s -> s.email().equals("tenant@example.com"))));
  }

  @Test
  @DisplayName(
      "provider failure leaves the request FAILED, not stuck PENDING with no signers, and"
          + " surfaces a plain message that never names the signing provider — the provider's own"
          + " exception message reaches the user-facing ProblemDetail verbatim otherwise (see"
          + " GlobalExceptionHandler.handleExternalService)")
  void providerFailureMarksRequestFailed() {
    when(providerClient.createSubmission(any(byte[].class), any(String.class), anyList()))
        .thenThrow(new ExternalServiceException("Documenso unreachable"));
    when(signatureRequestRepository.save(any()))
        .thenAnswer(
            invocation -> {
              var request = (com.buurman.domain.SignatureRequest) invocation.getArgument(0);
              if (request.getId() == null) {
                request.setId(UUID.randomUUID());
                request.setIdentifier(Optional.of(Sid.of("SGR00000000000000000000002")));
              }
              return request;
            });

    assertThatThrownBy(
            () ->
                service.createSignatureRequest(
                    ContractIdentifier.of("CON00000000000000000000001"),
                    DocumentIdentifier.of("DOC00000000000000000000001"),
                    principal))
        .isInstanceOf(ExternalServiceException.class)
        .hasMessageNotContaining("Documenso")
        .cause()
        .hasMessageContaining("Documenso unreachable");

    org.mockito.Mockito.verify(signatureRequestRepository, org.mockito.Mockito.times(2))
        .save(any());
    org.mockito.Mockito.verify(signatureSignerRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  @DisplayName(
      "getSignatureRequest resolves the documentIdentifier fresh from the request's own"
          + " documentId, ignoring the (untrusted) path parameter")
  void getSignatureRequestIgnoresPathDocumentIdentifier() {
    UUID requestId = UUID.randomUUID();
    com.buurman.domain.SignatureRequest existingRequest =
        com.buurman.domain.SignatureRequest.builder()
            .id(requestId)
            .identifier(Optional.of(Sid.of("SGR00000000000000000000003")))
            .teamId(TEAM_ID)
            .documentId(DOCUMENT_ID)
            .provider("documenso")
            .providerSubmissionId("envelope_9")
            .status(SignatureRequestStatus.PENDING)
            .build();
    when(signatureRequestRepository.getByIdentifierAndTeamId(
            any(SignatureRequestIdentifier.class), org.mockito.ArgumentMatchers.eq(TEAM_ID)))
        .thenReturn(existingRequest);
    when(signatureSignerRepository.findBySignatureRequestId(requestId)).thenReturn(List.of());

    // The document actually linked to the request (looked up by its real documentId) has an
    // identifier that is deliberately different from whatever the URL's documentIdentifier path
    // parameter says below.
    Document realDocument =
        Document.builder()
            .id(DOCUMENT_ID)
            .teamId(TEAM_ID)
            .identifier(Optional.of(Sid.of("DOC00000000000000000000099")))
            .fileKey("k")
            .fileName("addendum.pdf")
            .build();
    when(documentRepository.findByIdAndTeamId(DOCUMENT_ID, TEAM_ID))
        .thenReturn(Optional.of(realDocument));

    var response =
        service.getSignatureRequest(
            ContractIdentifier.of("CON00000000000000000000001"),
            // A "wrong"/stale documentIdentifier in the URL — must be ignored entirely.
            DocumentIdentifier.of("DOC00000000000000000000001"),
            SignatureRequestIdentifier.of("SGR00000000000000000000003"),
            principal);

    assertThat(response.status()).isEqualTo(SignatureRequestStatus.PENDING);
    assertThat(response.documentIdentifier().value()).isEqualTo("DOC00000000000000000000099");
  }

  @Test
  @DisplayName(
      "persists the tenant signer's contactId, and leaves it empty for the landlord (a TeamMember,"
          + " not a Contact)")
  void persistsTenantContactId() {
    when(providerClient.createSubmission(any(byte[].class), any(String.class), anyList()))
        .thenReturn(
            new SignatureSubmission(
                "envelope_3",
                List.of(
                    new ProviderSigner("1", "landlord@example.com"),
                    new ProviderSigner("2", "tenant@example.com"))));
    when(signatureRequestRepository.save(any()))
        .thenAnswer(
            invocation -> {
              var request = (com.buurman.domain.SignatureRequest) invocation.getArgument(0);
              request.setId(UUID.randomUUID());
              request.setIdentifier(Optional.of(Sid.of("SGR00000000000000000000005")));
              return request;
            });
    when(signatureSignerRepository.save(any()))
        .thenAnswer(
            invocation -> {
              var signer = (SignatureSigner) invocation.getArgument(0);
              signer.setId(UUID.randomUUID());
              return signer;
            });

    service.createSignatureRequest(
        ContractIdentifier.of("CON00000000000000000000001"),
        DocumentIdentifier.of("DOC00000000000000000000001"),
        principal);

    var saved = org.mockito.ArgumentCaptor.forClass(SignatureSigner.class);
    org.mockito.Mockito.verify(signatureSignerRepository, org.mockito.Mockito.times(2))
        .save(saved.capture());
    assertThat(saved.getAllValues())
        .extracting(SignatureSigner::getEmail, SignatureSigner::getContactId)
        .containsExactly(
            org.assertj.core.api.Assertions.tuple("landlord@example.com", Optional.empty()),
            org.assertj.core.api.Assertions.tuple("tenant@example.com", Optional.of(CONTACT_ID)));
  }

  @Test
  @DisplayName("listSignatureRequests returns every request for the document, newest first")
  void listSignatureRequestsReturnsAllForDocument() {
    Document document =
        Document.builder()
            .id(DOCUMENT_ID)
            .teamId(TEAM_ID)
            .identifier(Optional.of(Sid.of("DOC00000000000000000000001")))
            .fileKey("k")
            .fileName("addendum.pdf")
            .build();
    when(documentRepository.getByIdentifierAndTeamId(
            any(Sid.class), org.mockito.ArgumentMatchers.eq(TEAM_ID)))
        .thenReturn(document);

    UUID newerId = UUID.randomUUID();
    UUID olderId = UUID.randomUUID();
    when(signatureRequestRepository.findByDocumentIdAndTeamId(DOCUMENT_ID, TEAM_ID))
        .thenReturn(
            List.of(
                com.buurman.domain.SignatureRequest.builder()
                    .id(newerId)
                    .identifier(Optional.of(Sid.of("SGR00000000000000000000007")))
                    .teamId(TEAM_ID)
                    .documentId(DOCUMENT_ID)
                    .provider("documenso")
                    .providerSubmissionId("envelope_new")
                    .status(SignatureRequestStatus.PENDING)
                    .build(),
                com.buurman.domain.SignatureRequest.builder()
                    .id(olderId)
                    .identifier(Optional.of(Sid.of("SGR00000000000000000000006")))
                    .teamId(TEAM_ID)
                    .documentId(DOCUMENT_ID)
                    .provider("documenso")
                    .providerSubmissionId("envelope_old")
                    .status(SignatureRequestStatus.DECLINED)
                    .build()));
    when(signatureSignerRepository.findBySignatureRequestId(any())).thenReturn(List.of());

    var responses =
        service.listSignatureRequests(
            ContractIdentifier.of("CON00000000000000000000001"),
            DocumentIdentifier.of("DOC00000000000000000000001"),
            principal);

    assertThat(responses)
        .extracting(
            r -> r.identifier().value(), com.buurman.dto.response.SignatureRequestResponse::status)
        .containsExactly(
            org.assertj.core.api.Assertions.tuple(
                "SGR00000000000000000000007", SignatureRequestStatus.PENDING),
            org.assertj.core.api.Assertions.tuple(
                "SGR00000000000000000000006", SignatureRequestStatus.DECLINED));
    assertThat(responses)
        .allSatisfy(
            r ->
                assertThat(r.documentIdentifier().value()).isEqualTo("DOC00000000000000000000001"));
  }

  @Test
  @DisplayName("listSignatureRequests on another team's document is not found — no cross-team leak")
  void listSignatureRequestsCrossTeamDocumentIsNotFound() {
    when(documentRepository.getByIdentifierAndTeamId(
            any(Sid.class), org.mockito.ArgumentMatchers.eq(TEAM_ID)))
        .thenThrow(new NotFoundException("Document not found"));

    assertThatThrownBy(
            () ->
                service.listSignatureRequests(
                    ContractIdentifier.of("CON00000000000000000000001"),
                    DocumentIdentifier.of("DOC00000000000000000000001"),
                    principal))
        .isInstanceOf(NotFoundException.class);

    verifyNoInteractions(signatureRequestRepository);
  }

  @Test
  @DisplayName("cancelSignatureRequest calls the provider, then flips the request to CANCELLED")
  void cancelSignatureRequestCancelsPendingRequest() {
    UUID requestId = UUID.randomUUID();
    com.buurman.domain.SignatureRequest existingRequest =
        com.buurman.domain.SignatureRequest.builder()
            .id(requestId)
            .identifier(Optional.of(Sid.of("SGR00000000000000000000010")))
            .teamId(TEAM_ID)
            .documentId(DOCUMENT_ID)
            .provider("documenso")
            .providerSubmissionId("envelope_cancel")
            .status(SignatureRequestStatus.PENDING)
            .updatedBy(USER_ID)
            .build();
    when(signatureRequestRepository.getByIdentifierAndTeamId(
            any(SignatureRequestIdentifier.class), org.mockito.ArgumentMatchers.eq(TEAM_ID)))
        .thenReturn(existingRequest);
    when(signatureRequestRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(signatureSignerRepository.findBySignatureRequestId(requestId)).thenReturn(List.of());
    Document document =
        Document.builder()
            .id(DOCUMENT_ID)
            .teamId(TEAM_ID)
            .identifier(Optional.of(Sid.of("DOC00000000000000000000001")))
            .fileKey("k")
            .fileName("addendum.pdf")
            .build();
    when(documentRepository.findByIdAndTeamId(DOCUMENT_ID, TEAM_ID))
        .thenReturn(Optional.of(document));

    var response =
        service.cancelSignatureRequest(
            ContractIdentifier.of("CON00000000000000000000001"),
            DocumentIdentifier.of("DOC00000000000000000000001"),
            SignatureRequestIdentifier.of("SGR00000000000000000000010"),
            Optional.of("Tenant backed out"),
            principal);

    assertThat(response.status()).isEqualTo(SignatureRequestStatus.CANCELLED);
    org.mockito.Mockito.verify(providerClient)
        .cancelSubmission("envelope_cancel", "Tenant backed out");
  }

  @Test
  @DisplayName("cancelSignatureRequest rejects a request that already reached a final state")
  void cancelSignatureRequestRejectsTerminalRequest() {
    com.buurman.domain.SignatureRequest completedRequest =
        com.buurman.domain.SignatureRequest.builder()
            .id(UUID.randomUUID())
            .identifier(Optional.of(Sid.of("SGR00000000000000000000011")))
            .teamId(TEAM_ID)
            .documentId(DOCUMENT_ID)
            .provider("documenso")
            .providerSubmissionId("envelope_done")
            .status(SignatureRequestStatus.COMPLETED)
            .build();
    when(signatureRequestRepository.getByIdentifierAndTeamId(
            any(SignatureRequestIdentifier.class), org.mockito.ArgumentMatchers.eq(TEAM_ID)))
        .thenReturn(completedRequest);

    assertThatThrownBy(
            () ->
                service.cancelSignatureRequest(
                    ContractIdentifier.of("CON00000000000000000000001"),
                    DocumentIdentifier.of("DOC00000000000000000000001"),
                    SignatureRequestIdentifier.of("SGR00000000000000000000011"),
                    Optional.empty(),
                    principal))
        .isInstanceOf(BusinessRuleException.class);

    verifyNoInteractions(providerClient);
  }

  @Test
  @DisplayName(
      "cancelSignatureRequest leaves the request untouched and hides the provider's name when"
          + " the provider call fails")
  void cancelSignatureRequestWrapsProviderFailure() {
    com.buurman.domain.SignatureRequest existingRequest =
        com.buurman.domain.SignatureRequest.builder()
            .id(UUID.randomUUID())
            .identifier(Optional.of(Sid.of("SGR00000000000000000000012")))
            .teamId(TEAM_ID)
            .documentId(DOCUMENT_ID)
            .provider("documenso")
            .providerSubmissionId("envelope_fail")
            .status(SignatureRequestStatus.PENDING)
            .build();
    when(signatureRequestRepository.getByIdentifierAndTeamId(
            any(SignatureRequestIdentifier.class), org.mockito.ArgumentMatchers.eq(TEAM_ID)))
        .thenReturn(existingRequest);
    org.mockito.Mockito.doThrow(new ExternalServiceException("Documenso unreachable"))
        .when(providerClient)
        .cancelSubmission("envelope_fail", null);

    assertThatThrownBy(
            () ->
                service.cancelSignatureRequest(
                    ContractIdentifier.of("CON00000000000000000000001"),
                    DocumentIdentifier.of("DOC00000000000000000000001"),
                    SignatureRequestIdentifier.of("SGR00000000000000000000012"),
                    Optional.empty(),
                    principal))
        .isInstanceOf(ExternalServiceException.class)
        .hasMessageNotContaining("Documenso")
        .cause()
        .hasMessageContaining("Documenso unreachable");

    org.mockito.Mockito.verify(signatureRequestRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  @DisplayName(
      "dedupes signer emails when two contract parties resolve to the same email"
          + " (case-insensitive)")
  void dedupesDuplicateSignerEmails() {
    UUID otherContactId = UUID.randomUUID();
    ContractParty firstTenantParty =
        ContractParty.builder()
            .contractId(CONTRACT_ID)
            .contactId(Optional.of(CONTACT_ID))
            .role(ContractPartyRole.PRIMARY_TENANT)
            .build();
    ContractParty secondTenantParty =
        ContractParty.builder()
            .contractId(CONTRACT_ID)
            .contactId(Optional.of(otherContactId))
            .role(ContractPartyRole.GUARANTOR)
            .build();
    when(contractPartyRepository.findByContractIdAndTeamId(CONTRACT_ID, TEAM_ID))
        .thenReturn(List.of(firstTenantParty, secondTenantParty));

    // Same email as the CONTACT_ID contact stubbed in setUp(), just a different case.
    Contact sameEmailDifferentCase =
        Contact.builder().id(otherContactId).email(Optional.of("TENANT@example.com")).build();
    when(contactRepository.findByIdAndTeamId(otherContactId, TEAM_ID))
        .thenReturn(Optional.of(sameEmailDifferentCase));

    when(providerClient.createSubmission(any(byte[].class), any(String.class), anyList()))
        .thenReturn(
            new SignatureSubmission(
                "envelope_2",
                List.of(
                    new ProviderSigner("1", "landlord@example.com"),
                    new ProviderSigner("2", "tenant@example.com"))));
    when(signatureRequestRepository.save(any()))
        .thenAnswer(
            invocation -> {
              var request = (com.buurman.domain.SignatureRequest) invocation.getArgument(0);
              request.setId(UUID.randomUUID());
              request.setIdentifier(Optional.of(Sid.of("SGR00000000000000000000004")));
              return request;
            });
    when(signatureSignerRepository.save(any()))
        .thenAnswer(
            invocation -> {
              var signer = (SignatureSigner) invocation.getArgument(0);
              signer.setId(UUID.randomUUID());
              return signer;
            });

    service.createSignatureRequest(
        ContractIdentifier.of("CON00000000000000000000001"),
        DocumentIdentifier.of("DOC00000000000000000000001"),
        principal);

    org.mockito.Mockito.verify(providerClient)
        .createSubmission(
            any(byte[].class),
            any(String.class),
            org.mockito.ArgumentMatchers.argThat(signers -> signers.size() == 2));
    org.mockito.Mockito.verify(signatureSignerRepository, org.mockito.Mockito.times(2)).save(any());
  }
}
