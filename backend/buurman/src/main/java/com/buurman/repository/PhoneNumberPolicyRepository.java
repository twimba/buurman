package com.buurman.repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PhoneNumberPolicy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PhoneNumberPolicyRepository {

  private final DSLContext dsl;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  private static final org.jooq.Table<?> TABLE = table("phone_number_policy");
  private static final org.jooq.Field<UUID> ID = field("id", UUID.class);
  private static final org.jooq.Field<JSONB> POLICY_MATRIX = field("policy_matrix", JSONB.class);
  private static final org.jooq.Field<Integer> MAX_CODES_PER_HOUR =
      field("max_codes_per_hour", Integer.class);
  private static final org.jooq.Field<Integer> VERIFICATION_CODE_EXPIRY_MINUTES =
      field("verification_code_expiry_minutes", Integer.class);
  private static final org.jooq.Field<Timestamp> UPDATED_AT = field("updated_at", Timestamp.class);
  private static final org.jooq.Field<String> UPDATED_BY = field("updated_by", String.class);

  public Optional<PhoneNumberPolicy> findCurrent() {
    return dsl.select().from(TABLE).limit(1).fetchOptional().map(this::toDomain);
  }

  public PhoneNumberPolicy save(PhoneNumberPolicy policy) {
    Timestamp now = Timestamp.from(clock.instant());
    Timestamp updatedAt = policy.getUpdatedAt().map(Timestamp::from).orElse(now);

    dsl.update(TABLE)
        .set(POLICY_MATRIX, toJsonbMap(policy.getPolicyMatrix().orElse(null)))
        .set(MAX_CODES_PER_HOUR, policy.getMaxCodesPerHour())
        .set(VERIFICATION_CODE_EXPIRY_MINUTES, policy.getVerificationCodeExpiryMinutes())
        .set(UPDATED_AT, updatedAt)
        .set(UPDATED_BY, policy.getUpdatedBy().orElse(null))
        .where(ID.eq(policy.getId()))
        .execute();

    policy.setUpdatedAt(Optional.of(updatedAt.toInstant()));
    return policy;
  }

  private PhoneNumberPolicy toDomain(Record record) {
    PhoneNumberPolicy policy = new PhoneNumberPolicy();
    policy.setId(record.get(ID));
    policy.setPolicyMatrix(Optional.ofNullable(fromJsonbMap(record.get(POLICY_MATRIX))));
    Integer maxCodes = record.get(MAX_CODES_PER_HOUR);
    if (maxCodes != null) {
      policy.setMaxCodesPerHour(maxCodes);
    }
    Integer expiryMinutes = record.get(VERIFICATION_CODE_EXPIRY_MINUTES);
    if (expiryMinutes != null) {
      policy.setVerificationCodeExpiryMinutes(expiryMinutes);
    }
    Timestamp updatedAtTs = record.get(UPDATED_AT);
    if (updatedAtTs != null) {
      policy.setUpdatedAt(Optional.of(updatedAtTs.toInstant()));
    }
    policy.setUpdatedBy(Optional.of(record.get(UPDATED_BY)));
    return policy;
  }

  private JSONB toJsonbMap(@Nullable Map<String, List<String>> map) {
    try {
      return JSONB.valueOf(objectMapper.writeValueAsString(map != null ? map : Map.of()));
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize policy matrix to JSONB", e);
    }
  }

  private @Nullable Map<String, List<String>> fromJsonbMap(@Nullable JSONB jsonb) {
    if (jsonb == null) {
      return null;
    }
    try {
      return objectMapper.readValue(jsonb.data(), new TypeReference<>() {});
    } catch (Exception e) {
      throw new RuntimeException("Failed to deserialize policy matrix from JSONB", e);
    }
  }
}
