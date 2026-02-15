package com.buurman.repository;

import com.buurman.domain.RegistrationInvitation;
import com.buurman.dto.request.PageRequest;
import com.buurman.util.PaginationHelper;
import static com.buurman.util.UlidGenerator.newRegistrationInvitationId;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.REGISTRATION_INVITATIONS;

import static java.time.ZoneOffset.UTC;

@Repository
@RequiredArgsConstructor
public class RegistrationInvitationRepository {

    private final DSLContext dsl;
    private final Clock clock;

    public RegistrationInvitation save(RegistrationInvitation invitation) {
        LocalDateTime now = LocalDateTime.now(clock);
        UUID id = UUID.randomUUID();
        String identifier = newRegistrationInvitationId().value();

        dsl.insertInto(REGISTRATION_INVITATIONS)
                .set(REGISTRATION_INVITATIONS.ID, id)
                .set(REGISTRATION_INVITATIONS.IDENTIFIER, identifier)
                .set(REGISTRATION_INVITATIONS.CODE, invitation.getCode())
                .set(REGISTRATION_INVITATIONS.MAX_USAGES, invitation.getMaxUsages())
                .set(REGISTRATION_INVITATIONS.USAGE_COUNT, 0)
                .set(REGISTRATION_INVITATIONS.EXPIRES_AT, invitation.getExpiresAt() != null
                        ? LocalDateTime.ofInstant(invitation.getExpiresAt(), UTC) : null)
                .set(REGISTRATION_INVITATIONS.CREATED_AT, now)
                .set(REGISTRATION_INVITATIONS.UPDATED_AT, now)
                .set(REGISTRATION_INVITATIONS.CREATED_BY, invitation.getCreatedBy())
                .execute();

        invitation.setId(id);
        invitation.setIdentifier(identifier);
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

    public Optional<RegistrationInvitation> findByIdentifier(String identifier) {
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

    public boolean existsByCode(String code) {
        return dsl.fetchExists(
                dsl.selectFrom(REGISTRATION_INVITATIONS)
                        .where(REGISTRATION_INVITATIONS.CODE.eq(code))
        );
    }

    public PaginationHelper.PaginatedResult<RegistrationInvitation> findAllPaginated(
            PageRequest pageRequest, String search) {
        Condition condition = org.jooq.impl.DSL.trueCondition();

        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase() + "%";
            condition = condition.and(
                    REGISTRATION_INVITATIONS.CODE.lower().like(pattern)
                            .or(REGISTRATION_INVITATIONS.CREATED_BY.lower().like(pattern))
            );
        }

        Map<String, Field<?>> sortableFields = Map.of(
                "createdAt", REGISTRATION_INVITATIONS.CREATED_AT,
                "code", REGISTRATION_INVITATIONS.CODE,
                "usageCount", REGISTRATION_INVITATIONS.USAGE_COUNT
        );

        return PaginationHelper.paginate(dsl, REGISTRATION_INVITATIONS, condition,
                sortableFields, REGISTRATION_INVITATIONS.CREATED_AT, pageRequest,
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
     * Atomically increments usage_count only if the invitation is still valid.
     * Returns the number of affected rows (0 = invitation invalid or race condition).
     */
    public int incrementUsageAtomically(UUID id) {
        LocalDateTime now = LocalDateTime.now(clock);
        return dsl.update(REGISTRATION_INVITATIONS)
                .set(REGISTRATION_INVITATIONS.USAGE_COUNT, REGISTRATION_INVITATIONS.USAGE_COUNT.plus(1))
                .set(REGISTRATION_INVITATIONS.UPDATED_AT, now)
                .where(REGISTRATION_INVITATIONS.ID.eq(id))
                .and(REGISTRATION_INVITATIONS.REVOKED_AT.isNull())
                .and(REGISTRATION_INVITATIONS.MAX_USAGES.isNull()
                        .or(REGISTRATION_INVITATIONS.USAGE_COUNT.lt(REGISTRATION_INVITATIONS.MAX_USAGES)))
                .and(REGISTRATION_INVITATIONS.EXPIRES_AT.isNull()
                        .or(REGISTRATION_INVITATIONS.EXPIRES_AT.gt(now)))
                .execute();
    }

    private RegistrationInvitation toDomain(org.jooq.Record record) {
        RegistrationInvitation inv = new RegistrationInvitation();
        inv.setId(record.get(REGISTRATION_INVITATIONS.ID));
        inv.setIdentifier(record.get(REGISTRATION_INVITATIONS.IDENTIFIER));
        inv.setCode(record.get(REGISTRATION_INVITATIONS.CODE));
        inv.setMaxUsages(record.get(REGISTRATION_INVITATIONS.MAX_USAGES));
        inv.setUsageCount(record.get(REGISTRATION_INVITATIONS.USAGE_COUNT));
        inv.setExpiresAt(toInstant(record.get(REGISTRATION_INVITATIONS.EXPIRES_AT)));
        inv.setRevokedAt(toInstant(record.get(REGISTRATION_INVITATIONS.REVOKED_AT)));
        inv.setRevokedBy(record.get(REGISTRATION_INVITATIONS.REVOKED_BY));
        inv.setCreatedAt(toInstant(record.get(REGISTRATION_INVITATIONS.CREATED_AT)));
        inv.setUpdatedAt(toInstant(record.get(REGISTRATION_INVITATIONS.UPDATED_AT)));
        inv.setCreatedBy(record.get(REGISTRATION_INVITATIONS.CREATED_BY));
        return inv;
    }

    private static java.time.Instant toInstant(LocalDateTime ldt) {
        return ldt != null ? ldt.toInstant(UTC) : null;
    }
}
