package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.SignatureRequest;
import com.buurman.domain.SignatureRequestStatus;
import com.buurman.domain.SignatureSigner;
import com.buurman.domain.SignatureSignerRole;
import com.buurman.domain.SignatureSignerStatus;
import com.buurman.mapper.SignatureRequestRecordMapperImpl;
import com.buurman.mapper.SignatureSignerRecordMapperImpl;

@DisplayName("SignatureSignerRepository")
class SignatureSignerRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private SignatureSignerRepository repository;
  private UUID signatureRequestId;

  @BeforeEach
  void setUp() {
    repository = new SignatureSignerRepository(dsl, new SignatureSignerRecordMapperImpl(), CLOCK);
    SignatureRequestRepository requestRepository =
        new SignatureRequestRepository(dsl, new SignatureRequestRecordMapperImpl(), CLOCK);
    UUID propertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    UUID contractId = TestDataHelper.insertContract(dsl, TEAM_A_ID, propertyId, USER_ID);
    UUID documentId = TestDataHelper.insertDocument(dsl, TEAM_A_ID, contractId, USER_ID);
    SignatureRequest request =
        requestRepository.save(
            SignatureRequest.builder()
                .teamId(TEAM_A_ID)
                .documentId(documentId)
                .provider("documenso")
                .providerSubmissionId("envelope_test")
                .status(SignatureRequestStatus.PENDING)
                .createdBy(USER_ID)
                .updatedBy(USER_ID)
                .build());
    signatureRequestId = request.getId();
  }

  @Test
  @DisplayName("saves signers and lists them back in creation order")
  void savesAndLists() {
    SignatureSigner landlord =
        repository.save(
            SignatureSigner.builder()
                .signatureRequestId(signatureRequestId)
                .teamId(TEAM_A_ID)
                .email("landlord@example.com")
                .role(SignatureSignerRole.LANDLORD)
                .providerSignerId("1")
                .status(SignatureSignerStatus.PENDING)
                .build());
    SignatureSigner tenant =
        repository.save(
            SignatureSigner.builder()
                .signatureRequestId(signatureRequestId)
                .teamId(TEAM_A_ID)
                .email("tenant@example.com")
                .role(SignatureSignerRole.TENANT)
                .providerSignerId("2")
                .status(SignatureSignerStatus.PENDING)
                .build());

    var signers = repository.findBySignatureRequestId(signatureRequestId, TEAM_A_ID);
    assertThat(signers)
        .extracting(SignatureSigner::getId)
        .containsExactly(landlord.getId(), tenant.getId());
  }

  @Test
  @DisplayName("updateStatus sets status and signedAt")
  void updateStatusSetsSignedAt() {
    SignatureSigner signer =
        repository.save(
            SignatureSigner.builder()
                .signatureRequestId(signatureRequestId)
                .teamId(TEAM_A_ID)
                .email("tenant@example.com")
                .role(SignatureSignerRole.TENANT)
                .providerSignerId("3")
                .status(SignatureSignerStatus.PENDING)
                .build());

    Instant signedAt = Instant.parse("2026-03-01T13:00:00Z");
    repository.updateStatus(
        signer.getId(), TEAM_A_ID, SignatureSignerStatus.SIGNED, Optional.of(signedAt));

    var reloaded = repository.findBySignatureRequestId(signatureRequestId, TEAM_A_ID);
    assertThat(reloaded).hasSize(1);
    assertThat(reloaded.get(0).getStatus()).isEqualTo(SignatureSignerStatus.SIGNED);
    assertThat(reloaded.get(0).getSignedAt()).contains(signedAt);
  }

  @Test
  @DisplayName(
      "a query scoped to one team cannot see another team's signers or update their status")
  void crossTeamIsolation() {
    SignatureSigner signer =
        repository.save(
            SignatureSigner.builder()
                .signatureRequestId(signatureRequestId)
                .teamId(TEAM_A_ID)
                .email("tenant@example.com")
                .role(SignatureSignerRole.TENANT)
                .providerSignerId("4")
                .status(SignatureSignerStatus.PENDING)
                .build());

    assertThat(repository.findBySignatureRequestId(signatureRequestId, TEAM_B_ID)).isEmpty();
    assertThat(
            repository.findBySignatureRequestIds(java.util.List.of(signatureRequestId), TEAM_B_ID))
        .isEmpty();

    repository.updateStatus(
        signer.getId(), TEAM_B_ID, SignatureSignerStatus.SIGNED, Optional.empty());
    var stillPending = repository.findBySignatureRequestId(signatureRequestId, TEAM_A_ID);
    assertThat(stillPending).hasSize(1);
    assertThat(stillPending.get(0).getStatus()).isEqualTo(SignatureSignerStatus.PENDING);
  }
}
