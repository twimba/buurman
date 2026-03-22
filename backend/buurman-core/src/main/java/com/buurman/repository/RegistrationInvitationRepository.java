package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.REGISTRATION_INVITATIONS;
import static com.buurman.util.SidGenerator.newRegistrationInvitationId;
import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.lower;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.RegistrationInvitation;
import com.buurman.domain.Sid;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.util.PaginationHelper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RegistrationInvitationRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public RegistrationInvitation save(RegistrationInvitation invitation) {
    LocalDateTime now = LocalDateTime.now(clock);
    UUID id = UUID.randomUUID();
    Sid identifier = newRegistrationInvitationId();

    dsl.insertInto(REGISTRATION_INVITATIONS)
        .set(REGISTRATION_INVITATIONS.ID, id)
        .set(REGISTRATION_INVITATIONS.IDENTIFIER, identifier)
        .set(REGISTRATION_INVITATIONS.CODE, invitation.getCode())
        .set(REGISTRATION_INVITATIONS.MAX_USAGES, invitation.getMaxUsages().orElse(null))
        .set(REGISTRATION_INVITATIONS.USAGE_COUNT, 0)
        .set(
            REGISTRATION_INVITATIONS.EXPIRES_AT,
            invitation.getExpiresAt().map(i -> LocalDateTime.ofInstant(i, UTC)).orElse(null))
        .set(REGISTRATION_INVITATIONS.CREATED_AT, now)
        .set(REGISTRATION_INVITATIONS.UPDATED_AT, now)
        .set(REGISTRATION_INVITATIONS.CREATED_BY, invitation.getCreatedBy())
        .set(REGISTRATION_INVITATIONS.NOTE, invitation.getNote().orElse(null))
        .execute();

    invitation.setId(id);
    invitation.setIdentifier(java.util.Optional.of(identifier));
    invitation.setUsageCount(0);
    invitation.setCreatedAt(now.toInstant(UTC));
    invitation.setUpdatedAt(now.toInstant(UTC));
    return invitation;
  }

  public Optional<RegistrationInvitation> findByCode(String code) {
    return dsl.selectFrom(REGISTRATION_INVITATIONS)
        .where(REGISTRATION_INVITATIONS.CODE.eq(code))
        .fetchOptional()
        .map(this::toDomain);
  }

  public Optional<RegistrationInvitation> findByIdentifier(Sid identifier) {
    return dsl.selectFrom(REGISTRATION_INVITATIONS)
        .where(REGISTRATION_INVITATIONS.IDENTIFIER.eq(identifier))
        .fetchOptional()
        .map(this::toDomain);
  }

  public Optional<RegistrationInvitation> findById(UUID id) {
    return dsl.selectFrom(REGISTRATION_INVITATIONS)
        .where(REGISTRATION_INVITATIONS.ID.eq(id))
        .fetchOptional()
        .map(this::toDomain);
  }

  public RegistrationInvitation getByIdentifier(Sid identifier) {
    return findByIdentifier(identifier)
        .orElseThrow(() -> new NotFoundException("Registration invitation not found"));
  }

  public RegistrationInvitation getById(UUID id) {
    return findById(id)
        .orElseThrow(() -> new NotFoundException("Registration invitation not found"));
  }

  public boolean existsByCode(String code) {
    return dsl.fetchExists(
        dsl.selectFrom(REGISTRATION_INVITATIONS).where(REGISTRATION_INVITATIONS.CODE.eq(code)));
  }

  public PaginationHelper.PaginatedResult<RegistrationInvitation> findAllPaginated(
      PageRequest pageRequest, @Nullable String search) {
    Condition condition = org.jooq.impl.DSL.trueCondition();

    if (search != null && !search.isBlank()) {
      String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
      condition =
          condition.and(
              lower(REGISTRATION_INVITATIONS.CODE)
                  .like(pattern)
                  .or(lower(REGISTRATION_INVITATIONS.CREATED_BY).like(pattern)));
    }

    Map<String, Field<?>> sortableFields =
        Map.of(
            "createdAt", REGISTRATION_INVITATIONS.CREATED_AT,
            "code", REGISTRATION_INVITATIONS.CODE,
            "usageCount", REGISTRATION_INVITATIONS.USAGE_COUNT);

    return PaginationHelper.paginate(
        dsl,
        REGISTRATION_INVITATIONS,
        condition,
        sortableFields,
        REGISTRATION_INVITATIONS.CREATED_AT,
        pageRequest,
        r -> toDomain(r.into(REGISTRATION_INVITATIONS)));
  }

  public void revoke(UUID id, String revokedBy) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(REGISTRATION_INVITATIONS)
        .set(REGISTRATION_INVITATIONS.REVOKED_AT, now)
        .set(REGISTRATION_INVITATIONS.REVOKED_BY, revokedBy)
        .set(REGISTRATION_INVITATIONS.UPDATED_AT, now)
        .where(REGISTRATION_INVITATIONS.ID.eq(id))
        .execute();
  }

  /**
   * Atomically increments usage_count only if the invitation is still valid. Returns the number of
   * affected rows (0 = invitation invalid or race condition).
   */
  public int incrementUsageAtomically(UUID id) {
    LocalDateTime now = LocalDateTime.now(clock);
    return dsl.update(REGISTRATION_INVITATIONS)
        .set(REGISTRATION_INVITATIONS.USAGE_COUNT, REGISTRATION_INVITATIONS.USAGE_COUNT.plus(1))
        .set(REGISTRATION_INVITATIONS.UPDATED_AT, now)
        .where(REGISTRATION_INVITATIONS.ID.eq(id))
        .and(REGISTRATION_INVITATIONS.REVOKED_AT.isNull())
        .and(
            REGISTRATION_INVITATIONS
                .MAX_USAGES
                .isNull()
                .or(REGISTRATION_INVITATIONS.USAGE_COUNT.lt(REGISTRATION_INVITATIONS.MAX_USAGES)))
        .and(
            REGISTRATION_INVITATIONS
                .EXPIRES_AT
                .isNull()
                .or(REGISTRATION_INVITATIONS.EXPIRES_AT.gt(now)))
        .execute();
  }

  public void updateNote(UUID id, @Nullable String note) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(REGISTRATION_INVITATIONS)
        .set(REGISTRATION_INVITATIONS.NOTE, note)
        .set(REGISTRATION_INVITATIONS.UPDATED_AT, now)
        .where(REGISTRATION_INVITATIONS.ID.eq(id))
        .execute();
  }

  private RegistrationInvitation toDomain(org.jooq.Record record) {
    RegistrationInvitation inv = new RegistrationInvitation();
    inv.setId(record.get(REGISTRATION_INVITATIONS.ID));
    inv.setIdentifier(java.util.Optional.of(record.get(REGISTRATION_INVITATIONS.IDENTIFIER)));
    inv.setCode(record.get(REGISTRATION_INVITATIONS.CODE));
    inv.setMaxUsages(Optional.ofNullable(record.get(REGISTRATION_INVITATIONS.MAX_USAGES)));
    inv.setUsageCount(record.get(REGISTRATION_INVITATIONS.USAGE_COUNT));
    inv.setExpiresAt(
        Optional.ofNullable(toInstant(record.get(REGISTRATION_INVITATIONS.EXPIRES_AT))));
    inv.setRevokedAt(
        Optional.ofNullable(toInstant(record.get(REGISTRATION_INVITATIONS.REVOKED_AT))));
    inv.setRevokedBy(Optional.ofNullable(record.get(REGISTRATION_INVITATIONS.REVOKED_BY)));
    inv.setCreatedAt(record.get(REGISTRATION_INVITATIONS.CREATED_AT).toInstant(UTC));
    inv.setUpdatedAt(record.get(REGISTRATION_INVITATIONS.UPDATED_AT).toInstant(UTC));
    inv.setCreatedBy(record.get(REGISTRATION_INVITATIONS.CREATED_BY));
    inv.setNote(Optional.ofNullable(record.get(REGISTRATION_INVITATIONS.NOTE)));
    return inv;
  }

  private static java.time.@Nullable Instant toInstant(@Nullable LocalDateTime ldt) {
    return ldt != null ? ldt.toInstant(UTC) : null;
  }
}
