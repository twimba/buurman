package com.buurman.service.esignature;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractParty;
import com.buurman.domain.Document;
import com.buurman.domain.Sid;
import com.buurman.domain.SignatureRequest;
import com.buurman.domain.SignatureRequestStatus;
import com.buurman.domain.SignatureSigner;
import com.buurman.domain.SignatureSignerRole;
import com.buurman.domain.SignatureSignerStatus;
import com.buurman.domain.identifier.ContractIdentifier;
import com.buurman.domain.identifier.DocumentIdentifier;
import com.buurman.domain.identifier.SignatureRequestIdentifier;
import com.buurman.dto.response.SignatureRequestResponse;
import com.buurman.dto.response.SignatureSignerResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.exception.ExternalServiceException;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractPartyRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.DocumentRepository;
import com.buurman.repository.SignatureRequestRepository;
import com.buurman.repository.SignatureSignerRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.service.FeatureFlagService;
import com.buurman.service.S3StorageService;
import com.buurman.util.FeatureFlags;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignatureService {

  private static final String PROVIDER = "documenso";

  private static final java.util.Set<SignatureRequestStatus> TERMINAL_STATUSES =
      java.util.Set.of(
          SignatureRequestStatus.COMPLETED,
          SignatureRequestStatus.DECLINED,
          SignatureRequestStatus.CANCELLED,
          SignatureRequestStatus.FAILED);

  private final FeatureFlagService featureFlagService;
  private final DocumentRepository documentRepository;
  private final ContractRepository contractRepository;
  private final ContractPartyRepository contractPartyRepository;
  private final ContactRepository contactRepository;
  private final S3StorageService s3StorageService;
  private final SignatureProviderClient providerClient;
  private final SignatureRequestRepository signatureRequestRepository;
  private final SignatureSignerRepository signatureSignerRepository;

  /**
   * Sends a document for e-signature.
   *
   * <p>Deliberately NOT {@code @Transactional}, and it must not be made so. This method's own
   * try/catch is written to guarantee that a provider outage leaves a {@code FAILED} row behind
   * rather than nothing: it inserts the request {@code PENDING}, calls S3 and Documenso, then
   * either flips the row to {@code FAILED} and rethrows, or records the envelope id and its
   * signers. Under a single transaction the rethrow would trigger Spring's rollback advice and
   * discard <em>every</em> write in the invocation — the {@code FAILED} update and the {@code
   * PENDING} insert alike — so a provider outage would leave no trace of the attempt at all, the
   * exact opposite of the intent. Without the annotation each jOOQ statement auto-commits as it
   * happens, which is what the control flow above assumes.
   *
   * <p>Same reasoning (and the same "no transaction across a blocking third-party round-trip" rule)
   * as {@code CostService.snapshotNow}, {@code FxRateService.backfill}, {@code
   * TakeoutService.processTakeout} and {@code NotificationOutboxJob.processEntry}.
   */
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public SignatureRequestResponse createSignatureRequest(
      ContractIdentifier contractIdentifier,
      DocumentIdentifier documentIdentifier,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    if (!featureFlagService.isEnabled(FeatureFlags.ESIGNATURE_ENABLED, principal)) {
      throw new BusinessRuleException("E-signature is not enabled for this team");
    }

    Document document = documentRepository.getByIdentifierAndTeamId(documentIdentifier, teamId);
    Contract contract = contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);

    // Each signer gets a "placeholder" — the literal PDF text DocumensoClient asks Documenso to
    // locate to place that signer's signature field. "signature-landlord" for the landlord,
    // "signature-tenant-N" for the Nth distinct tenant email encountered (1-based) — this must
    // match LetterExporterHelper.signatureBlocks' numbering exactly, since both sides
    // independently iterate contractPartyRepository.findByContractIdAndTeamId in the same order,
    // and signatureBlocks has no way to know in advance who will eventually click "send" (it
    // renders the PDF's placeholders before any signature request exists). Tenants are therefore
    // numbered here by distinct TENANT email alone, the same rule signatureBlocks uses — seeding
    // the dedup set with the landlord's own email before numbering tenants (as this used to)
    // shifts every tenant after a collision down by one in this method but not in the PDF's own
    // labels, misattributing a real signer's field.
    SignerRequest landlordSigner =
        new SignerRequest(
            principal.getEmail(),
            principal.getName(),
            SignatureSignerRole.LANDLORD,
            "signature-landlord");
    String landlordEmailKey = principal.getEmail().toLowerCase(Locale.ROOT);

    // The Buurman-side contact a tenant signer came from is kept here, keyed by the same
    // lower-cased email, so the persisted SignatureSigner can carry it.
    Map<String, UUID> contactIdByEmail = new LinkedHashMap<>();

    Map<String, SignerRequest> tenantSignersByEmail = new LinkedHashMap<>();
    List<ContractParty> parties =
        contractPartyRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    int[] tenantIndex = {0};
    for (ContractParty party : parties) {
      Optional<UUID> partyContactId = party.getContactId();
      partyContactId
          .flatMap(contactId -> contactRepository.findByIdAndTeamId(contactId, teamId))
          .flatMap(Contact::getEmail)
          .ifPresent(
              email -> {
                String emailKey = email.toLowerCase(Locale.ROOT);
                if (tenantSignersByEmail.containsKey(emailKey)) {
                  return;
                }
                tenantIndex[0]++;
                tenantSignersByEmail.put(
                    emailKey,
                    new SignerRequest(
                        email,
                        email,
                        SignatureSignerRole.TENANT,
                        "signature-tenant-" + tenantIndex[0]));
                partyContactId.ifPresent(contactId -> contactIdByEmail.put(emailKey, contactId));
              });
    }

    // Only now, after every tenant's number is final, drop a tenant whose email matches the
    // sender's — Documenso rejects (or double-sends) a duplicate recipient email, and the
    // landlord's own recipient already covers it. This does not renumber anyone: the dropped
    // tenant's placeholder simply gets no field, every other tenant's number is unaffected, and
    // signatureBlocks' PDF labels stay aligned with this list for every signer that is actually
    // sent.
    tenantSignersByEmail.remove(landlordEmailKey);

    List<SignerRequest> signerRequests = new ArrayList<>();
    signerRequests.add(landlordSigner);
    signerRequests.addAll(tenantSignersByEmail.values());

    SignatureRequest request =
        signatureRequestRepository.save(
            SignatureRequest.builder()
                .teamId(teamId)
                .documentId(document.getId())
                .provider(PROVIDER)
                .providerSubmissionId("pending-" + UUID.randomUUID())
                .status(SignatureRequestStatus.PENDING)
                .createdBy(principal.getUserId())
                .updatedBy(principal.getUserId())
                .build());

    SignatureSubmission submission;
    try {
      byte[] pdfBytes = readAllBytes(s3StorageService.downloadFile(document.getFileKey()));
      submission =
          providerClient.createSubmission(pdfBytes, document.getFileName(), signerRequests);
    } catch (RuntimeException e) {
      request.setStatus(SignatureRequestStatus.FAILED);
      request.setUpdatedBy(principal.getUserId());
      signatureRequestRepository.save(request);
      // The provider's own exception message (e.g. "Documenso did not return an envelope id")
      // names the third-party service and is meaningless to a landlord — it still reaches the
      // logs via this exception's cause, but the end user gets a plain, actionable message
      // instead. Re-thrown as-is, this would surface verbatim in the UI toast (see
      // GlobalExceptionHandler.handleExternalService, which copies ex.getMessage() straight into
      // the response).
      throw new ExternalServiceException(
          "We couldn't send this document for signature. Please try again in a few minutes —"
              + " if it keeps happening, contact support.",
          e);
    }

    request.setProviderSubmissionId(submission.providerSubmissionId());
    signatureRequestRepository.save(request);

    Map<String, String> emailByProviderSignerId =
        submission.signers().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    ProviderSigner::providerSignerId, ProviderSigner::email));
    List<SignatureSigner> savedSigners = new ArrayList<>();
    for (SignerRequest signerRequest : signerRequests) {
      Optional<String> matchedProviderSignerId =
          emailByProviderSignerId.entrySet().stream()
              .filter(e -> e.getValue().equalsIgnoreCase(signerRequest.email()))
              .map(Map.Entry::getKey)
              .findFirst();
      if (matchedProviderSignerId.isEmpty()) {
        // Without a provider id, this signer's webhook events can never be matched back to this
        // row (the webhook keys recipients by provider id), so their status stays PENDING forever.
        // Loud on purpose: silently degrading to the email hides a provider contract change.
        log.warn(
            "Documenso returned no recipient matching signer {} on envelope {} — falling back to"
                + " the email as providerSignerId; webhook status updates for this signer will"
                + " not match",
            signerRequest.email(),
            submission.providerSubmissionId());
      }
      String providerSignerId = matchedProviderSignerId.orElse(signerRequest.email());
      savedSigners.add(
          signatureSignerRepository.save(
              SignatureSigner.builder()
                  .signatureRequestId(request.getId())
                  .contactId(
                      Optional.ofNullable(
                          contactIdByEmail.get(signerRequest.email().toLowerCase(Locale.ROOT))))
                  .email(signerRequest.email())
                  .role(signerRequest.role())
                  .providerSignerId(providerSignerId)
                  .status(SignatureSignerStatus.PENDING)
                  .build()));
    }

    log.info(
        "Sent document {} for signature: request {} ({} signers)",
        documentIdentifier.value(),
        request.getIdentifier().orElseThrow().value(),
        savedSigners.size());

    return toResponse(request, documentIdentifier, savedSigners);
  }

  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public SignatureRequestResponse getSignatureRequest(
      ContractIdentifier contractIdentifier,
      DocumentIdentifier documentIdentifier,
      SignatureRequestIdentifier signatureRequestIdentifier,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    // contractIdentifier/documentIdentifier are path parameters used only for team-scoped
    // routing — they are NOT trusted as data. The request is looked up by its own team-scoped
    // identifier, and the response's documentIdentifier is always resolved fresh from the
    // request's own persisted documentId below, never from these caller-supplied values (a
    // stale link or copy-pasted URL must never be able to make the response lie about which
    // document a signature request is actually for).
    SignatureRequest request =
        signatureRequestRepository.getByIdentifierAndTeamId(signatureRequestIdentifier, teamId);
    List<SignatureSigner> signers =
        signatureSignerRepository.findBySignatureRequestId(request.getId());
    Sid resolvedDocumentIdentifier = resolveDocumentIdentifier(request.getDocumentId(), teamId);
    return toResponse(request, resolvedDocumentIdentifier, signers);
  }

  /**
   * Every signature request ever raised for a document, newest first. The frontend calls this on
   * mount so a page reload cannot make it forget an in-flight request and offer "Send for
   * signature" a second time on a document that is already out for signing.
   */
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public List<SignatureRequestResponse> listSignatureRequests(
      ContractIdentifier contractIdentifier,
      DocumentIdentifier documentIdentifier,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    // Team-scoped via the document and contract lookups, same as createSignatureRequest: an
    // identifier belonging to another team is a 404 before any signature row is touched.
    Document document = documentRepository.getByIdentifierAndTeamId(documentIdentifier, teamId);
    contractRepository.getByIdentifierAndTeamId(contractIdentifier, teamId);
    Sid resolvedDocumentIdentifier = document.getIdentifier().orElseThrow();

    List<SignatureRequest> requests =
        signatureRequestRepository.findByDocumentIdAndTeamId(document.getId(), teamId);
    Map<UUID, List<SignatureSigner>> signersByRequestId =
        signatureSignerRepository
            .findBySignatureRequestIds(requests.stream().map(SignatureRequest::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(SignatureSigner::getSignatureRequestId));

    return requests.stream()
        .map(
            request ->
                toResponse(
                    request,
                    resolvedDocumentIdentifier,
                    signersByRequestId.getOrDefault(request.getId(), List.of())))
        .toList();
  }

  /**
   * Retracts a request that has not yet reached a final state. The provider call happens before the
   * local status flip, same reasoning as {@link #createSignatureRequest}: if Documenso is
   * unreachable the request must stay exactly as it was, not silently show as CANCELLED locally
   * while the signers can still sign it.
   */
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public SignatureRequestResponse cancelSignatureRequest(
      ContractIdentifier contractIdentifier,
      DocumentIdentifier documentIdentifier,
      SignatureRequestIdentifier signatureRequestIdentifier,
      Optional<String> reason,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    // contractIdentifier/documentIdentifier are path parameters used only for team-scoped
    // routing, same as getSignatureRequest — the request is looked up by its own identifier.
    SignatureRequest request =
        signatureRequestRepository.getByIdentifierAndTeamId(signatureRequestIdentifier, teamId);

    if (TERMINAL_STATUSES.contains(request.getStatus())) {
      throw new BusinessRuleException(
          "This signature request is already "
              + request.getStatus().name().toLowerCase(Locale.ROOT)
              + " and can no longer be cancelled");
    }

    try {
      providerClient.cancelSubmission(request.getProviderSubmissionId(), reason.orElse(null));
    } catch (RuntimeException e) {
      throw new ExternalServiceException(
          "We couldn't retract this signature request. Please try again in a few minutes — if"
              + " it keeps happening, contact support.",
          e);
    }

    request.setStatus(SignatureRequestStatus.CANCELLED);
    request.setUpdatedBy(principal.getUserId());
    signatureRequestRepository.save(request);

    List<SignatureSigner> signers =
        signatureSignerRepository.findBySignatureRequestId(request.getId());
    Sid resolvedDocumentIdentifier = resolveDocumentIdentifier(request.getDocumentId(), teamId);

    log.info("Cancelled signature request {}", request.getIdentifier().orElseThrow().value());

    return toResponse(request, resolvedDocumentIdentifier, signers);
  }

  private Sid resolveDocumentIdentifier(UUID documentId, UUID teamId) {
    return documentRepository
        .findByIdAndTeamId(documentId, teamId)
        .flatMap(Document::getIdentifier)
        .orElseThrow();
  }

  private SignatureRequestResponse toResponse(
      SignatureRequest request, Sid documentIdentifier, List<SignatureSigner> signers) {
    Optional<Sid> signedDocumentIdentifier =
        request
            .getSignedDocumentId()
            .flatMap(id -> documentRepository.findByIdAndTeamId(id, request.getTeamId()))
            .flatMap(Document::getIdentifier);

    List<SignatureSignerResponse> signerResponses =
        signers.stream()
            .map(
                s ->
                    new SignatureSignerResponse(
                        s.getEmail(), s.getRole(), s.getStatus(), s.getSignedAt()))
            .toList();

    return new SignatureRequestResponse(
        request.getIdentifier().orElseThrow(),
        documentIdentifier,
        signedDocumentIdentifier,
        request.getStatus(),
        signerResponses,
        request.getCreatedAt(),
        request.getUpdatedAt());
  }

  private static byte[] readAllBytes(InputStream in) {
    try (in;
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      in.transferTo(out);
      return out.toByteArray();
    } catch (java.io.IOException e) {
      throw new com.buurman.exception.ExternalServiceException(
          "Failed to read document from storage", e);
    }
  }
}
