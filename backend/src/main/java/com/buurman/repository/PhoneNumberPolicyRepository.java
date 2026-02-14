package com.buurman.repository;

import com.buurman.domain.PhoneNumberPolicy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import static org.jooq.impl.DSL.*;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PhoneNumberPolicyRepository {

    private final DSLContext dsl;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private static final org.jooq.Table<?> TABLE = table("phone_number_policy");
    private static final org.jooq.Field<UUID> ID = field("id", UUID.class);
    private static final org.jooq.Field<JSONB> POLICY_MATRIX = field("policy_matrix", JSONB.class);
    private static final org.jooq.Field<Integer> MAX_CODES_PER_HOUR = field("max_codes_per_hour", Integer.class);
    private static final org.jooq.Field<Integer> VERIFICATION_CODE_EXPIRY_MINUTES = field("verification_code_expiry_minutes", Integer.class);
    private static final org.jooq.Field<Timestamp> UPDATED_AT = field("updated_at", Timestamp.class);
    private static final org.jooq.Field<String> UPDATED_BY = field("updated_by", String.class);

    public PhoneNumberPolicyRepository(DSLContext dsl, ObjectMapper objectMapper, Clock clock) {
        this.dsl = dsl;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public Optional<PhoneNumberPolicy> findCurrent() {
        return dsl.select()
                .from(TABLE)
                .limit(1)
                .fetchOptional()
                .map(this::toDomain);
    }

    public PhoneNumberPolicy save(PhoneNumberPolicy policy) {
        Timestamp now = Timestamp.from(clock.instant());
        Timestamp updatedAt = policy.getUpdatedAt() != null
                ? Timestamp.from(policy.getUpdatedAt())
                : now;

        dsl.update(TABLE)
                .set(POLICY_MATRIX, toJsonbMap(policy.getPolicyMatrix()))
                .set(MAX_CODES_PER_HOUR, policy.getMaxCodesPerHour())
                .set(VERIFICATION_CODE_EXPIRY_MINUTES, policy.getVerificationCodeExpiryMinutes())
                .set(UPDATED_AT, updatedAt)
                .set(UPDATED_BY, policy.getUpdatedBy())
                .where(ID.eq(policy.getId()))
                .execute();

        policy.setUpdatedAt(updatedAt.toInstant());
        return policy;
    }

    private PhoneNumberPolicy toDomain(Record record) {
        PhoneNumberPolicy policy = new PhoneNumberPolicy();
        policy.setId(record.get(ID));
        policy.setPolicyMatrix(fromJsonbMap(record.get(POLICY_MATRIX)));
        Integer maxCodes = record.get(MAX_CODES_PER_HOUR);
        if (maxCodes != null) policy.setMaxCodesPerHour(maxCodes);
        Integer expiryMinutes = record.get(VERIFICATION_CODE_EXPIRY_MINUTES);
        if (expiryMinutes != null) policy.setVerificationCodeExpiryMinutes(expiryMinutes);
        Timestamp updatedAtTs = record.get(UPDATED_AT);
        if (updatedAtTs != null) {
            policy.setUpdatedAt(updatedAtTs.toInstant());
        }
        policy.setUpdatedBy(record.get(UPDATED_BY));
        return policy;
    }

    private JSONB toJsonbMap(Map<String, List<String>> map) {
        try {
            return JSONB.valueOf(objectMapper.writeValueAsString(map != null ? map : Map.of()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize policy matrix to JSONB", e);
        }
    }

    private Map<String, List<String>> fromJsonbMap(JSONB jsonb) {
        if (jsonb == null) return Map.of();
        try {
            return objectMapper.readValue(jsonb.data(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize policy matrix from JSONB", e);
        }
    }
}
