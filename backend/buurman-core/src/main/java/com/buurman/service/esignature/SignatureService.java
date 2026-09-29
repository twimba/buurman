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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  private final FeatureFlagService featureFlagService;
  private final DocumentRepository documentRepository;
  private final ContractRepository contractRepository;
  private final ContractPartyRepository contractPartyRepository;
  private final ContactRepository contactRepository;
  private final S3StorageService s3StorageService;
  private final SignatureProviderClient providerClient;
  private final SignatureRequestRepository signatureRequestRepository;
  private final SignatureSignerRepository signatureSignerRepository;

  @Transactional
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

    // Keyed by lower-cased email to dedupe: a contract party resolving to the same address as
    // another party, or as the landlord's own email, must never produce two signer entries for
    // the same person (Documenso may error, or double-send, on a duplicate signer email).
    Map<String, SignerRequest> signerRequestsByEmail = new LinkedHashMap<>();
    signerRequestsByEmail.put(
        principal.getEmail().toLowerCase(Locale.ROOT),
        new SignerRequest(principal.getEmail(), principal.getName(), SignatureSignerRole.LANDLORD));

    List<ContractParty> parties =
        contractPartyRepository.findByContractIdAndTeamId(contract.getId(), teamId);
    for (ContractParty party : parties) {
      party
          .getContactId()
          .flatMap(contactId -> contactRepository.findByIdAndTeamId(contactId, teamId))
          .flatMap(Contact::getEmail)
          .ifPresent(
              email ->
                  signerRequestsByEmail.putIfAbsent(
                      email.toLowerCase(Locale.ROOT),
                      new SignerRequest(email, email, SignatureSignerRole.TENANT)));
    }
    List<SignerRequest> signerRequests = new ArrayList<>(signerRequestsByEmail.values());

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
      throw e;
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
      String providerSignerId =
          emailByProviderSignerId.entrySet().stream()
              .filter(e -> e.getValue().equalsIgnoreCase(signerRequest.email()))
              .map(Map.Entry::getKey)
              .findFirst()
              .orElse(signerRequest.email());
      savedSigners.add(
          signatureSignerRepository.save(
              SignatureSigner.builder()
                  .signatureRequestId(request.getId())
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
