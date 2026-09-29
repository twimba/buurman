package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.SIGNATURE_REQUESTS;
import static com.buurman.util.SidGenerator.newSignatureRequestId;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Sid;
import com.buurman.domain.SignatureRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.SignatureRequestRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SignatureRequestRepository {

  private final DSLContext dsl;
  private final SignatureRequestRecordMapper mapper;
  private final Clock clock;

  public SignatureRequest save(SignatureRequest request) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (request.getId() == null) {
      UUID newId = UUID.randomUUID();
      Sid identifier = newSignatureRequestId();

      dsl.insertInto(SIGNATURE_REQUESTS)
          .set(SIGNATURE_REQUESTS.ID, newId)
          .set(SIGNATURE_REQUESTS.IDENTIFIER, identifier)
          .set(SIGNATURE_REQUESTS.TEAM_ID, request.getTeamId())
          .set(SIGNATURE_REQUESTS.DOCUMENT_ID, request.getDocumentId())
          .set(SIGNATURE_REQUESTS.SIGNED_DOCUMENT_ID, request.getSignedDocumentId().orElse(null))
          .set(SIGNATURE_REQUESTS.PROVIDER, request.getProvider())
          .set(SIGNATURE_REQUESTS.PROVIDER_SUBMISSION_ID, request.getProviderSubmissionId())
          .set(SIGNATURE_REQUESTS.STATUS, request.getStatus().name())
          .set(SIGNATURE_REQUESTS.CREATED_AT, now)
          .set(SIGNATURE_REQUESTS.UPDATED_AT, now)
          .set(SIGNATURE_REQUESTS.CREATED_BY, request.getCreatedBy())
          .set(SIGNATURE_REQUESTS.UPDATED_BY, request.getUpdatedBy())
          .execute();

      request.setId(newId);
      request.setIdentifier(Optional.of(identifier));
      request.setCreatedAt(now.toInstant(UTC));
      request.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(SIGNATURE_REQUESTS)
          .set(SIGNATURE_REQUESTS.SIGNED_DOCUMENT_ID, request.getSignedDocumentId().orElse(null))
          .set(SIGNATURE_REQUESTS.STATUS, request.getStatus().name())
          .set(SIGNATURE_REQUESTS.UPDATED_AT, now)
          .set(SIGNATURE_REQUESTS.UPDATED_BY, request.getUpdatedBy())
          .where(
              SIGNATURE_REQUESTS
                  .ID
                  .eq(request.getId())
                  .and(SIGNATURE_REQUESTS.TEAM_ID.eq(request.getTeamId())))
          .execute();
      request.setUpdatedAt(now.toInstant(UTC));
    }

    return request;
  }

  public Optional<SignatureRequest> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(SIGNATURE_REQUESTS)
        .where(
            SIGNATURE_REQUESTS
                .IDENTIFIER
                .eq(identifier)
                .and(SIGNATURE_REQUESTS.TEAM_ID.eq(teamId))
                .and(SIGNATURE_REQUESTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public SignatureRequest getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Signature request not found"));
  }

  /**
   * Team-agnostic lookup used only by webhook processing, which has no authenticated team context.
   */
  public Optional<SignatureRequest> findByProviderAndProviderSubmissionId(
      String provider, String providerSubmissionId) {
    return dsl.selectFrom(SIGNATURE_REQUESTS)
        .where(
            SIGNATURE_REQUESTS
                .PROVIDER
                .eq(provider)
                .and(SIGNATURE_REQUESTS.PROVIDER_SUBMISSION_ID.eq(providerSubmissionId))
                .and(SIGNATURE_REQUESTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public List<SignatureRequest> findByDocumentIdAndTeamId(UUID documentId, UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(SIGNATURE_REQUESTS)
            .where(
                SIGNATURE_REQUESTS
                    .DOCUMENT_ID
                    .eq(documentId)
                    .and(SIGNATURE_REQUESTS.TEAM_ID.eq(teamId))
                    .and(SIGNATURE_REQUESTS.DELETED_AT.isNull()))
            .orderBy(SIGNATURE_REQUESTS.CREATED_AT.desc())
            .fetch()
            .map(mapper::toDomain));
  }
}
