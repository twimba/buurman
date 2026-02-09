package com.buurman.repository;

import com.buurman.domain.EmailVerificationCode;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.EMAIL_VERIFICATION_CODES;

@Repository
public class EmailVerificationCodeRepository {

    private final DSLContext dsl;

    public EmailVerificationCodeRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void save(EmailVerificationCode code) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        UUID id = UUID.randomUUID();

        dsl.insertInto(EMAIL_VERIFICATION_CODES)
                .set(EMAIL_VERIFICATION_CODES.ID, id)
                .set(EMAIL_VERIFICATION_CODES.USER_ID, code.getUserId())
                .set(EMAIL_VERIFICATION_CODES.CODE, code.getCode())
                .set(EMAIL_VERIFICATION_CODES.EXPIRES_AT, LocalDateTime.ofInstant(code.getExpiresAt(), ZoneOffset.UTC))
                .set(EMAIL_VERIFICATION_CODES.CREATED_AT, now)
                .execute();

        code.setId(id);
        code.setCreatedAt(now.toInstant(ZoneOffset.UTC));
    }

    public Optional<EmailVerificationCode> findValidCode(UUID userId, String code) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        return dsl.selectFrom(EMAIL_VERIFICATION_CODES)
                .where(EMAIL_VERIFICATION_CODES.USER_ID.eq(userId))
                .and(EMAIL_VERIFICATION_CODES.CODE.eq(code))
                .and(EMAIL_VERIFICATION_CODES.USED_AT.isNull())
                .and(EMAIL_VERIFICATION_CODES.EXPIRES_AT.gt(now))
                .orderBy(EMAIL_VERIFICATION_CODES.CREATED_AT.desc())
                .limit(1)
                .fetchOptional()
                .map(record -> {
                    EmailVerificationCode evc = new EmailVerificationCode();
                    evc.setId(record.getId());
                    evc.setUserId(record.getUserId());
                    evc.setCode(record.getCode());
                    evc.setExpiresAt(record.getExpiresAt().toInstant(ZoneOffset.UTC));
                    evc.setUsedAt(record.getUsedAt() != null ? record.getUsedAt().toInstant(ZoneOffset.UTC) : null);
                    evc.setCreatedAt(record.getCreatedAt().toInstant(ZoneOffset.UTC));
                    return evc;
                });
    }

    public void invalidateAllForUser(UUID userId) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        dsl.update(EMAIL_VERIFICATION_CODES)
                .set(EMAIL_VERIFICATION_CODES.USED_AT, now)
                .where(EMAIL_VERIFICATION_CODES.USER_ID.eq(userId))
                .and(EMAIL_VERIFICATION_CODES.USED_AT.isNull())
                .execute();
    }

    public int countRecentByUserId(UUID userId, Instant since) {
        LocalDateTime sinceLocal = LocalDateTime.ofInstant(since, ZoneOffset.UTC);

        return dsl.fetchCount(
                dsl.selectFrom(EMAIL_VERIFICATION_CODES)
                        .where(EMAIL_VERIFICATION_CODES.USER_ID.eq(userId))
                        .and(EMAIL_VERIFICATION_CODES.CREATED_AT.gt(sinceLocal))
        );
    }

    public void markUsed(UUID id) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        dsl.update(EMAIL_VERIFICATION_CODES)
                .set(EMAIL_VERIFICATION_CODES.USED_AT, now)
                .where(EMAIL_VERIFICATION_CODES.ID.eq(id))
                .and(EMAIL_VERIFICATION_CODES.USED_AT.isNull())
                .execute();
    }
}
