package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.SIGNATURE_SIGNERS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.SignatureSigner;
import com.buurman.domain.SignatureSignerStatus;
import com.buurman.mapper.SignatureSignerRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SignatureSignerRepository {

  private final DSLContext dsl;
  private final SignatureSignerRecordMapper mapper;
  private final Clock clock;

  public SignatureSigner save(SignatureSigner signer) {
    LocalDateTime now = LocalDateTime.now(clock);
    UUID newId = UUID.randomUUID();

    dsl.insertInto(SIGNATURE_SIGNERS)
        .set(SIGNATURE_SIGNERS.ID, newId)
        .set(SIGNATURE_SIGNERS.SIGNATURE_REQUEST_ID, signer.getSignatureRequestId())
        .set(SIGNATURE_SIGNERS.TEAM_ID, signer.getTeamId())
        .set(SIGNATURE_SIGNERS.CONTACT_ID, signer.getContactId().orElse(null))
        .set(SIGNATURE_SIGNERS.EMAIL, signer.getEmail())
        .set(SIGNATURE_SIGNERS.ROLE, signer.getRole().name())
        .set(SIGNATURE_SIGNERS.PROVIDER_SIGNER_ID, signer.getProviderSignerId())
        .set(SIGNATURE_SIGNERS.STATUS, signer.getStatus().name())
        .set(SIGNATURE_SIGNERS.CREATED_AT, now)
        .set(SIGNATURE_SIGNERS.UPDATED_AT, now)
        .execute();

    signer.setId(newId);
    signer.setCreatedAt(now.toInstant(UTC));
    signer.setUpdatedAt(now.toInstant(UTC));
    return signer;
  }

  public List<SignatureSigner> findBySignatureRequestId(UUID signatureRequestId, UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(SIGNATURE_SIGNERS)
            .where(
                SIGNATURE_SIGNERS
                    .SIGNATURE_REQUEST_ID
                    .eq(signatureRequestId)
                    .and(SIGNATURE_SIGNERS.TEAM_ID.eq(teamId)))
            .orderBy(SIGNATURE_SIGNERS.CREATED_AT.asc())
            .fetch()
            .map(mapper::toDomain));
  }

  public List<SignatureSigner> findBySignatureRequestIds(
      List<UUID> signatureRequestIds, UUID teamId) {
    if (signatureRequestIds.isEmpty()) {
      return List.of();
    }
    return List.copyOf(
        dsl.selectFrom(SIGNATURE_SIGNERS)
            .where(
                SIGNATURE_SIGNERS
                    .SIGNATURE_REQUEST_ID
                    .in(signatureRequestIds)
                    .and(SIGNATURE_SIGNERS.TEAM_ID.eq(teamId)))
            .orderBy(SIGNATURE_SIGNERS.CREATED_AT.asc())
            .fetch()
            .map(mapper::toDomain));
  }

  public void updateStatus(
      UUID id, UUID teamId, SignatureSignerStatus status, Optional<Instant> signedAt) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(SIGNATURE_SIGNERS)
        .set(SIGNATURE_SIGNERS.STATUS, status.name())
        .set(
            SIGNATURE_SIGNERS.SIGNED_AT,
            signedAt.map(i -> LocalDateTime.ofInstant(i, UTC)).orElse(null))
        .set(SIGNATURE_SIGNERS.UPDATED_AT, now)
        .where(SIGNATURE_SIGNERS.ID.eq(id).and(SIGNATURE_SIGNERS.TEAM_ID.eq(teamId)))
        .execute();
  }
}
