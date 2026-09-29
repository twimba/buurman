package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.SignatureRequest;
import com.buurman.domain.SignatureRequestStatus;
import com.buurman.mapper.SignatureRequestRecordMapperImpl;

@DisplayName("SignatureRequestRepository")
class SignatureRequestRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private SignatureRequestRepository repository;
  private UUID teamADocumentId;
  private UUID teamBDocumentId;

  @BeforeEach
  void setUp() {
    repository = new SignatureRequestRepository(dsl, new SignatureRequestRecordMapperImpl(), CLOCK);
    UUID teamAPropertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    UUID teamBPropertyId = TestDataHelper.insertProperty(dsl, TEAM_B_ID, USER_ID);
    UUID teamAContractId = TestDataHelper.insertContract(dsl, TEAM_A_ID, teamAPropertyId, USER_ID);
    UUID teamBContractId = TestDataHelper.insertContract(dsl, TEAM_B_ID, teamBPropertyId, USER_ID);
    teamADocumentId = TestDataHelper.insertDocument(dsl, TEAM_A_ID, teamAContractId, USER_ID);
    teamBDocumentId = TestDataHelper.insertDocument(dsl, TEAM_B_ID, teamBContractId, USER_ID);
  }

  private SignatureRequest newRequest(UUID teamId, UUID documentId) {
    return SignatureRequest.builder()
        .teamId(teamId)
        .documentId(documentId)
        .provider("documenso")
        .providerSubmissionId("envelope_" + UUID.randomUUID())
        .status(SignatureRequestStatus.PENDING)
        .createdBy(USER_ID)
        .updatedBy(USER_ID)
        .build();
  }

  @Test
  @DisplayName(
      "saves and re-reads a request, and a team-B lookup by a team-A identifier finds nothing")
  void teamIsolation() {
    SignatureRequest saved = repository.save(newRequest(TEAM_A_ID, teamADocumentId));
    assertThat(saved.getIdentifier()).isPresent();

    var foundOwnTeam =
        repository.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);
    assertThat(foundOwnTeam).isPresent();

    var foundWrongTeam =
        repository.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_B_ID);
    assertThat(foundWrongTeam).isEmpty();
  }

  @Test
  @DisplayName("findByProviderAndProviderSubmissionId is team-agnostic, for webhook processing")
  void findsByProviderSubmissionIdAcrossTeams() {
    SignatureRequest saved = repository.save(newRequest(TEAM_B_ID, teamBDocumentId));

    var found =
        repository.findByProviderAndProviderSubmissionId(
            "documenso", saved.getProviderSubmissionId());

    assertThat(found).isPresent();
    assertThat(found.orElseThrow().getTeamId()).isEqualTo(TEAM_B_ID);
  }

  @Test
  @DisplayName("save() on an existing request updates status and signedDocumentId in place")
  void updateExistingRequest() {
    SignatureRequest saved = repository.save(newRequest(TEAM_A_ID, teamADocumentId));

    saved.setStatus(SignatureRequestStatus.COMPLETED);
    saved.setSignedDocumentId(java.util.Optional.of(teamADocumentId));
    repository.save(saved);

    SignatureRequest reloaded =
        repository.getByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);
    assertThat(reloaded.getStatus()).isEqualTo(SignatureRequestStatus.COMPLETED);
    assertThat(reloaded.getSignedDocumentId()).contains(teamADocumentId);
  }
}
