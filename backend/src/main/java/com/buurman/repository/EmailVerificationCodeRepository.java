package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.EMAIL_VERIFICATION_CODES;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.EmailVerificationCode;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class EmailVerificationCodeRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public void save(EmailVerificationCode code) {
    LocalDateTime now = LocalDateTime.now(clock);
    UUID id = UUID.randomUUID();
    LocalDateTime createdAt = now;

    dsl.insertInto(EMAIL_VERIFICATION_CODES)
        .set(EMAIL_VERIFICATION_CODES.ID, id)
        .set(EMAIL_VERIFICATION_CODES.USER_ID, code.getUserId())
        .set(EMAIL_VERIFICATION_CODES.CODE, code.getCode())
        .set(EMAIL_VERIFICATION_CODES.EXPIRES_AT, LocalDateTime.ofInstant(code.getExpiresAt(), UTC))
        .set(EMAIL_VERIFICATION_CODES.CREATED_AT, createdAt)
        .execute();

    code.setId(id);
    code.setCreatedAt(createdAt.toInstant(UTC));
  }

  public Optional<EmailVerificationCode> findValidCode(UUID userId, String code) {
    LocalDateTime now = LocalDateTime.now(clock);

    return dsl.selectFrom(EMAIL_VERIFICATION_CODES)
        .where(EMAIL_VERIFICATION_CODES.USER_ID.eq(userId))
        .and(EMAIL_VERIFICATION_CODES.CODE.eq(code))
        .and(EMAIL_VERIFICATION_CODES.USED_AT.isNull())
        .and(EMAIL_VERIFICATION_CODES.EXPIRES_AT.gt(now))
        .orderBy(EMAIL_VERIFICATION_CODES.CREATED_AT.desc())
        .limit(1)
        .fetchOptional()
        .map(
            record -> {
              EmailVerificationCode evc = new EmailVerificationCode();
              evc.setId(record.getId());
              evc.setUserId(record.getUserId());
              evc.setCode(record.getCode());
              evc.setExpiresAt(record.getExpiresAt().toInstant(UTC));
              evc.setUsedAt(Optional.ofNullable(record.getUsedAt()).map(v -> v.toInstant(UTC)));
              evc.setCreatedAt(record.getCreatedAt().toInstant(UTC));
              return evc;
            });
  }

  public void invalidateAllForUser(UUID userId) {
    LocalDateTime now = LocalDateTime.now(clock);

    dsl.update(EMAIL_VERIFICATION_CODES)
        .set(EMAIL_VERIFICATION_CODES.USED_AT, now)
        .where(EMAIL_VERIFICATION_CODES.USER_ID.eq(userId))
        .and(EMAIL_VERIFICATION_CODES.USED_AT.isNull())
        .execute();
  }

  public int countRecentByUserId(UUID userId, Instant since) {
    LocalDateTime sinceLocal = LocalDateTime.ofInstant(since, UTC);

    return dsl.fetchCount(
        dsl.selectFrom(EMAIL_VERIFICATION_CODES)
            .where(EMAIL_VERIFICATION_CODES.USER_ID.eq(userId))
            .and(EMAIL_VERIFICATION_CODES.CREATED_AT.gt(sinceLocal)));
  }

  public int deleteExpiredAndUsed(Instant before) {
    LocalDateTime cutoff = LocalDateTime.ofInstant(before, UTC);

    return dsl.deleteFrom(EMAIL_VERIFICATION_CODES)
        .where(
            EMAIL_VERIFICATION_CODES
                .EXPIRES_AT
                .lt(cutoff)
                .or(
                    EMAIL_VERIFICATION_CODES
                        .USED_AT
                        .isNotNull()
                        .and(EMAIL_VERIFICATION_CODES.USED_AT.lt(cutoff))))
        .execute();
  }

  public void markUsed(UUID id) {
    LocalDateTime now = LocalDateTime.now(clock);

    dsl.update(EMAIL_VERIFICATION_CODES)
        .set(EMAIL_VERIFICATION_CODES.USED_AT, now)
        .where(EMAIL_VERIFICATION_CODES.ID.eq(id))
        .and(EMAIL_VERIFICATION_CODES.USED_AT.isNull())
        .execute();
  }
}
