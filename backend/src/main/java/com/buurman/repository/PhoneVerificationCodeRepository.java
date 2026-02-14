package com.buurman.repository;

import com.buurman.domain.PhoneVerificationCode;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.PHONE_VERIFICATION_CODES;

@Repository
public class PhoneVerificationCodeRepository {

    private final DSLContext dsl;
    private final Clock clock;

    public PhoneVerificationCodeRepository(DSLContext dsl, Clock clock) {
        this.dsl = dsl;
        this.clock = clock;
    }

    public void save(PhoneVerificationCode code) {
        LocalDateTime now = LocalDateTime.now(clock);
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = code.getCreatedAt() != null
                ? LocalDateTime.ofInstant(code.getCreatedAt(), ZoneOffset.UTC)
                : now;

        dsl.insertInto(PHONE_VERIFICATION_CODES)
                .set(PHONE_VERIFICATION_CODES.ID, id)
                .set(PHONE_VERIFICATION_CODES.USER_ID, code.getUserId())
                .set(PHONE_VERIFICATION_CODES.PHONE, code.getPhone())
                .set(PHONE_VERIFICATION_CODES.CODE, code.getCode())
                .set(PHONE_VERIFICATION_CODES.EXPIRES_AT, LocalDateTime.ofInstant(code.getExpiresAt(), ZoneOffset.UTC))
                .set(PHONE_VERIFICATION_CODES.CREATED_AT, createdAt)
                .execute();

        code.setId(id);
        code.setCreatedAt(createdAt.toInstant(ZoneOffset.UTC));
    }

    public Optional<PhoneVerificationCode> findValidCode(UUID userId, String code, String phone) {
        LocalDateTime now = LocalDateTime.now(clock);

        return dsl.selectFrom(PHONE_VERIFICATION_CODES)
                .where(PHONE_VERIFICATION_CODES.USER_ID.eq(userId))
                .and(PHONE_VERIFICATION_CODES.CODE.eq(code))
                .and(PHONE_VERIFICATION_CODES.PHONE.eq(phone))
                .and(PHONE_VERIFICATION_CODES.USED_AT.isNull())
                .and(PHONE_VERIFICATION_CODES.EXPIRES_AT.gt(now))
                .orderBy(PHONE_VERIFICATION_CODES.CREATED_AT.desc())
                .limit(1)
                .fetchOptional()
                .map(record -> {
                    PhoneVerificationCode pvc = new PhoneVerificationCode();
                    pvc.setId(record.getId());
                    pvc.setUserId(record.getUserId());
                    pvc.setPhone(record.getPhone());
                    pvc.setCode(record.getCode());
                    pvc.setExpiresAt(record.getExpiresAt().toInstant(ZoneOffset.UTC));
                    pvc.setUsedAt(record.getUsedAt() != null ? record.getUsedAt().toInstant(ZoneOffset.UTC) : null);
                    pvc.setCreatedAt(record.getCreatedAt().toInstant(ZoneOffset.UTC));
                    return pvc;
                });
    }

    public void invalidateAllForUser(UUID userId) {
        LocalDateTime now = LocalDateTime.now(clock);

        dsl.update(PHONE_VERIFICATION_CODES)
                .set(PHONE_VERIFICATION_CODES.USED_AT, now)
                .where(PHONE_VERIFICATION_CODES.USER_ID.eq(userId))
                .and(PHONE_VERIFICATION_CODES.USED_AT.isNull())
                .execute();
    }

    public int countRecentByUserId(UUID userId, Instant since) {
        LocalDateTime sinceLocal = LocalDateTime.ofInstant(since, ZoneOffset.UTC);

        return dsl.fetchCount(
                dsl.selectFrom(PHONE_VERIFICATION_CODES)
                        .where(PHONE_VERIFICATION_CODES.USER_ID.eq(userId))
                        .and(PHONE_VERIFICATION_CODES.CREATED_AT.gt(sinceLocal))
        );
    }

    public Optional<Instant> findMostRecentCreatedAt(UUID userId) {
        return dsl.select(PHONE_VERIFICATION_CODES.CREATED_AT)
                .from(PHONE_VERIFICATION_CODES)
                .where(PHONE_VERIFICATION_CODES.USER_ID.eq(userId))
                .orderBy(PHONE_VERIFICATION_CODES.CREATED_AT.desc())
                .limit(1)
                .fetchOptional()
                .map(r -> r.get(PHONE_VERIFICATION_CODES.CREATED_AT).toInstant(ZoneOffset.UTC));
    }

    public void markUsed(UUID id) {
        LocalDateTime now = LocalDateTime.now(clock);

        dsl.update(PHONE_VERIFICATION_CODES)
                .set(PHONE_VERIFICATION_CODES.USED_AT, now)
                .where(PHONE_VERIFICATION_CODES.ID.eq(id))
                .and(PHONE_VERIFICATION_CODES.USED_AT.isNull())
                .execute();
    }
}
