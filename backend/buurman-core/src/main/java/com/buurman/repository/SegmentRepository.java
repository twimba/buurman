package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.CALENDAR_FEEDS;
import static com.buurman.jooq.generated.Tables.CONTACTS;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.DOCUMENTS;
import static com.buurman.jooq.generated.Tables.EXPENSES;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static com.buurman.jooq.generated.Tables.PHOTOS;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.SEGMENTS;
import static com.buurman.jooq.generated.Tables.SEGMENT_CONDITIONS;
import static com.buurman.jooq.generated.Tables.TEAMS;
import static com.buurman.jooq.generated.Tables.TEAM_MEMBERS;
import static com.buurman.jooq.generated.Tables.TEAM_PREFERENCES;
import static com.buurman.jooq.generated.Tables.USERS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import com.buurman.domain.SegmentAttribute;
import com.buurman.domain.SegmentCondition;
import com.buurman.domain.SegmentDefinition;
import com.buurman.domain.SegmentOperator;
import com.buurman.domain.Sid;
import com.buurman.jooq.generated.tables.records.SegmentConditionsRecord;
import com.buurman.jooq.generated.tables.records.SegmentsRecord;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SegmentRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public List<SegmentDefinition> findAll() {
    List<SegmentsRecord> segmentRecords =
        dsl.selectFrom(SEGMENTS)
            .where(SEGMENTS.DELETED_AT.isNull())
            .orderBy(SEGMENTS.PRIORITY.asc(), SEGMENTS.KEY.asc())
            .fetch();

    if (segmentRecords.isEmpty()) {
      return List.of();
    }

    List<UUID> segmentIds = segmentRecords.stream().map(SegmentsRecord::getId).toList();

    Map<UUID, List<SegmentCondition>> conditionsBySegment =
        dsl
            .selectFrom(SEGMENT_CONDITIONS)
            .where(SEGMENT_CONDITIONS.SEGMENT_ID.in(segmentIds))
            .fetch()
            .stream()
            .map(this::toConditionDomain)
            .collect(Collectors.groupingBy(SegmentCondition::getSegmentId));

    return segmentRecords.stream()
        .map(r -> toSegmentDomain(r, conditionsBySegment.getOrDefault(r.getId(), List.of())))
        .toList();
  }

  public Optional<SegmentDefinition> findByKey(String key) {
    return dsl.selectFrom(SEGMENTS)
        .where(SEGMENTS.KEY.eq(key).and(SEGMENTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(
            r -> {
              List<SegmentCondition> conditions =
                  dsl.selectFrom(SEGMENT_CONDITIONS)
                      .where(SEGMENT_CONDITIONS.SEGMENT_ID.eq(r.getId()))
                      .fetch()
                      .map(this::toConditionDomain);
              return toSegmentDomain(r, conditions);
            });
  }

  public Optional<SegmentDefinition> findById(UUID id) {
    return dsl.selectFrom(SEGMENTS)
        .where(SEGMENTS.ID.eq(id).and(SEGMENTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(
            r -> {
              List<SegmentCondition> conditions =
                  dsl.selectFrom(SEGMENT_CONDITIONS)
                      .where(SEGMENT_CONDITIONS.SEGMENT_ID.eq(r.getId()))
                      .fetch()
                      .map(this::toConditionDomain);
              return toSegmentDomain(r, conditions);
            });
  }

  public SegmentDefinition save(SegmentDefinition segment, UUID actorId) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (segment.getId() == null) {
      UUID newId = UUID.randomUUID();
      dsl.insertInto(SEGMENTS)
          .set(SEGMENTS.ID, newId)
          .set(SEGMENTS.KEY, segment.getKey())
          .set(SEGMENTS.NAME, segment.getName())
          .set(SEGMENTS.DESCRIPTION, segment.getDescription().orElse(null))
          .set(SEGMENTS.PRIORITY, segment.getPriority())
          .set(SEGMENTS.CREATED_AT, now)
          .set(SEGMENTS.UPDATED_AT, now)
          .set(SEGMENTS.CREATED_BY, actorId)
          .set(SEGMENTS.UPDATED_BY, actorId)
          .execute();
      segment.setId(newId);
      segment.setCreatedAt(now.toInstant(UTC));
      segment.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(SEGMENTS)
          .set(SEGMENTS.NAME, segment.getName())
          .set(SEGMENTS.DESCRIPTION, segment.getDescription().orElse(null))
          .set(SEGMENTS.PRIORITY, segment.getPriority())
          .set(SEGMENTS.UPDATED_AT, now)
          .set(SEGMENTS.UPDATED_BY, actorId)
          .where(SEGMENTS.ID.eq(segment.getId()))
          .execute();
      segment.setUpdatedAt(now.toInstant(UTC));
    }
    return segment;
  }

  public void softDelete(UUID id, UUID actorId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(SEGMENTS)
        .set(SEGMENTS.DELETED_AT, now)
        .set(SEGMENTS.UPDATED_BY, actorId)
        .set(SEGMENTS.UPDATED_AT, now)
        .where(SEGMENTS.ID.eq(id).and(SEGMENTS.DELETED_AT.isNull()))
        .execute();
  }

  public void replaceConditions(UUID segmentId, List<SegmentCondition> conditions, UUID actorId) {
    LocalDateTime now = LocalDateTime.now(clock);

    // Delete existing conditions
    dsl.deleteFrom(SEGMENT_CONDITIONS).where(SEGMENT_CONDITIONS.SEGMENT_ID.eq(segmentId)).execute();

    // Insert new conditions
    for (SegmentCondition condition : conditions) {
      UUID condId = UUID.randomUUID();
      dsl.insertInto(SEGMENT_CONDITIONS)
          .set(SEGMENT_CONDITIONS.ID, condId)
          .set(SEGMENT_CONDITIONS.SEGMENT_ID, segmentId)
          .set(SEGMENT_CONDITIONS.ATTRIBUTE, condition.getAttribute().toDbValue())
          .set(SEGMENT_CONDITIONS.OPERATOR, condition.getOperator().toDbValue())
          .set(SEGMENT_CONDITIONS.VALUE, condition.getValue())
          .set(SEGMENT_CONDITIONS.CREATED_AT, now)
          .set(SEGMENT_CONDITIONS.UPDATED_AT, now)
          .set(SEGMENT_CONDITIONS.CREATED_BY, actorId)
          .set(SEGMENT_CONDITIONS.UPDATED_BY, actorId)
          .execute();
      condition.setId(condId);
      condition.setSegmentId(segmentId);
    }
  }

  /** Count teams matching all segment conditions. */
  public long countMatchingTeams(SegmentDefinition segment) {
    if (segment.getConditions().isEmpty()) {
      return 0;
    }

    Condition condition = TEAMS.DELETED_AT.isNull();

    for (SegmentCondition c : segment.getConditions()) {
      switch (c.getAttribute()) {
        case IS_DEMO -> condition = condition.and(buildBoolCondition(TEAMS.DEMO, c));
        case TEAM_AGE_DAYS -> {
          Field<Long> ageDays =
              DSL.field("EXTRACT(EPOCH FROM (now() - {0})) / 86400", Long.class, TEAMS.CREATED_AT);
          condition = condition.and(buildNumCondition(ageDays, c));
        }
        case PROPERTY_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(PROPERTIES)
                      .where(PROPERTIES.TEAM_ID.eq(TEAMS.ID).and(PROPERTIES.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case MEMBER_COUNT -> {
          var tmCount = TEAM_MEMBERS.as("tm_count");
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(tmCount)
                      .where(tmCount.TEAM_ID.eq(TEAMS.ID).and(tmCount.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case ROLE ->
            condition =
                condition.and(
                    DSL.exists(
                        dsl.selectOne()
                            .from(TEAM_MEMBERS)
                            .where(
                                TEAM_MEMBERS
                                    .TEAM_ID
                                    .eq(TEAMS.ID)
                                    .and(TEAM_MEMBERS.DELETED_AT.isNull())
                                    .and(buildStrCondition(TEAM_MEMBERS.ROLE, c)))));
        case IS_OWNER ->
            condition =
                condition.and(
                    DSL.exists(
                        dsl.selectOne()
                            .from(TEAM_MEMBERS)
                            .where(
                                TEAM_MEMBERS
                                    .TEAM_ID
                                    .eq(TEAMS.ID)
                                    .and(TEAM_MEMBERS.DELETED_AT.isNull())
                                    .and(buildBoolCondition(TEAM_MEMBERS.IS_OWNER, c)))));
        case CONTRACT_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(CONTRACTS)
                      .where(CONTRACTS.TEAM_ID.eq(TEAMS.ID).and(CONTRACTS.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case CONTACT_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(CONTACTS)
                      .where(CONTACTS.TEAM_ID.eq(TEAMS.ID).and(CONTACTS.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case PHOTO_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(PHOTOS)
                      .where(PHOTOS.TEAM_ID.eq(TEAMS.ID).and(PHOTOS.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case DOCUMENT_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(DOCUMENTS)
                      .where(DOCUMENTS.TEAM_ID.eq(TEAMS.ID).and(DOCUMENTS.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case EXPENSE_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(EXPENSES)
                      .where(EXPENSES.TEAM_ID.eq(TEAMS.ID).and(EXPENSES.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case PAYMENT_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(PAYMENTS)
                      .where(PAYMENTS.TEAM_ID.eq(TEAMS.ID).and(PAYMENTS.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case CALENDAR_FEED_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(CALENDAR_FEEDS)
                      .where(
                          CALENDAR_FEEDS
                              .TEAM_ID
                              .eq(TEAMS.ID)
                              .and(CALENDAR_FEEDS.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case EMAIL, EMAIL_VERIFIED -> {
          // User-level attributes: check if ANY member in the team matches
          condition = condition.and(buildUserExistsCondition(c));
        }
        case IS_TEAM_SCOPE -> condition = condition.and(buildScopeCondition(true, c));
        case IS_USER_SCOPE -> condition = condition.and(buildScopeCondition(false, c));
        case TEAM_NAME -> condition = condition.and(buildStrCondition(TEAMS.NAME, c));
        case TEAM_ADMIN_EMAIL -> condition = condition.and(buildTeamMemberEmailCondition(c, true));
        case TEAM_OWNER_EMAIL -> condition = condition.and(buildTeamMemberEmailCondition(c, false));
        case TEAM_CURRENCY ->
            condition =
                condition.and(buildTeamPreferencesCondition(TEAM_PREFERENCES.DEFAULT_CURRENCY, c));
        case TEAM_DEFAULT_COUNTRY ->
            condition =
                condition.and(
                    buildTeamPreferencesCondition(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE, c));
        case TEAM_TIMEZONE ->
            condition = condition.and(buildTeamPreferencesCondition(TEAM_PREFERENCES.TIMEZONE, c));
      }
    }

    return dsl.fetchCount(dsl.selectFrom(TEAMS).where(condition));
  }

  /** Count distinct users matching all segment conditions. */
  public long countMatchingUsers(SegmentDefinition segment) {
    if (segment.getConditions().isEmpty()) {
      return 0;
    }

    Condition teamCond = TEAMS.DELETED_AT.isNull();
    Condition memberCond = TEAM_MEMBERS.DELETED_AT.isNull();
    Condition userCond = USERS.DELETED_AT.isNull();

    for (SegmentCondition c : segment.getConditions()) {
      switch (c.getAttribute()) {
        case IS_DEMO -> teamCond = teamCond.and(buildBoolCondition(TEAMS.DEMO, c));
        case TEAM_AGE_DAYS -> {
          Field<Long> ageDays =
              DSL.field("EXTRACT(EPOCH FROM (now() - {0})) / 86400", Long.class, TEAMS.CREATED_AT);
          teamCond = teamCond.and(buildNumCondition(ageDays, c));
        }
        case PROPERTY_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(PROPERTIES)
                      .where(PROPERTIES.TEAM_ID.eq(TEAMS.ID).and(PROPERTIES.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case MEMBER_COUNT -> {
          var tmCount = TEAM_MEMBERS.as("tm_count");
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(tmCount)
                      .where(tmCount.TEAM_ID.eq(TEAMS.ID).and(tmCount.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case CONTRACT_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(CONTRACTS)
                      .where(CONTRACTS.TEAM_ID.eq(TEAMS.ID).and(CONTRACTS.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case CONTACT_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(CONTACTS)
                      .where(CONTACTS.TEAM_ID.eq(TEAMS.ID).and(CONTACTS.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case PHOTO_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(PHOTOS)
                      .where(PHOTOS.TEAM_ID.eq(TEAMS.ID).and(PHOTOS.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case DOCUMENT_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(DOCUMENTS)
                      .where(DOCUMENTS.TEAM_ID.eq(TEAMS.ID).and(DOCUMENTS.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case EXPENSE_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(EXPENSES)
                      .where(EXPENSES.TEAM_ID.eq(TEAMS.ID).and(EXPENSES.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case PAYMENT_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(PAYMENTS)
                      .where(PAYMENTS.TEAM_ID.eq(TEAMS.ID).and(PAYMENTS.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case CALENDAR_FEED_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(CALENDAR_FEEDS)
                      .where(
                          CALENDAR_FEEDS
                              .TEAM_ID
                              .eq(TEAMS.ID)
                              .and(CALENDAR_FEEDS.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case ROLE -> memberCond = memberCond.and(buildStrCondition(TEAM_MEMBERS.ROLE, c));
        case IS_OWNER -> memberCond = memberCond.and(buildBoolCondition(TEAM_MEMBERS.IS_OWNER, c));
        case EMAIL -> userCond = userCond.and(buildStrCondition(USERS.EMAIL, c));
        case EMAIL_VERIFIED -> {
          if ("true".equals(c.getValue())) {
            userCond = userCond.and(USERS.EMAIL_VERIFIED_AT.isNotNull());
          } else {
            userCond = userCond.and(USERS.EMAIL_VERIFIED_AT.isNull());
          }
        }
        case IS_TEAM_SCOPE -> teamCond = teamCond.and(buildScopeCondition(false, c));
        case IS_USER_SCOPE -> teamCond = teamCond.and(buildScopeCondition(true, c));
        case TEAM_NAME -> teamCond = teamCond.and(buildStrCondition(TEAMS.NAME, c));
        case TEAM_ADMIN_EMAIL -> teamCond = teamCond.and(buildTeamMemberEmailCondition(c, true));
        case TEAM_OWNER_EMAIL -> teamCond = teamCond.and(buildTeamMemberEmailCondition(c, false));
        case TEAM_CURRENCY ->
            teamCond =
                teamCond.and(buildTeamPreferencesCondition(TEAM_PREFERENCES.DEFAULT_CURRENCY, c));
        case TEAM_DEFAULT_COUNTRY ->
            teamCond =
                teamCond.and(
                    buildTeamPreferencesCondition(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE, c));
        case TEAM_TIMEZONE ->
            teamCond = teamCond.and(buildTeamPreferencesCondition(TEAM_PREFERENCES.TIMEZONE, c));
      }
    }

    // Join order: TEAMS -> TEAM_MEMBERS -> USERS ensures all table references are available
    Long result =
        dsl.selectCount()
            .from(
                dsl.selectDistinct(USERS.ID)
                    .from(TEAMS)
                    .join(TEAM_MEMBERS)
                    .on(TEAM_MEMBERS.TEAM_ID.eq(TEAMS.ID).and(memberCond))
                    .join(USERS)
                    .on(USERS.ID.eq(TEAM_MEMBERS.USER_ID).and(userCond))
                    .where(teamCond))
            .fetchOne(0, Long.class);
    return result != null ? result : 0L;
  }

  public record MatchingTeam(
      UUID id, Sid identifier, String teamName, boolean demo, Instant createdAt) {}

  public record MatchingUser(
      UUID id,
      Sid identifier,
      String email,
      String firstName,
      String lastName,
      String role,
      Sid teamIdentifier,
      String teamName) {}

  /** Find teams matching all segment conditions. Limited to 500. */
  public List<MatchingTeam> findMatchingTeams(SegmentDefinition segment) {
    if (segment.getConditions().isEmpty()) {
      return List.of();
    }

    Condition condition = TEAMS.DELETED_AT.isNull();

    for (SegmentCondition c : segment.getConditions()) {
      switch (c.getAttribute()) {
        case IS_DEMO -> condition = condition.and(buildBoolCondition(TEAMS.DEMO, c));
        case TEAM_AGE_DAYS -> {
          Field<Long> ageDays =
              DSL.field("EXTRACT(EPOCH FROM (now() - {0})) / 86400", Long.class, TEAMS.CREATED_AT);
          condition = condition.and(buildNumCondition(ageDays, c));
        }
        case PROPERTY_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(PROPERTIES)
                      .where(PROPERTIES.TEAM_ID.eq(TEAMS.ID).and(PROPERTIES.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case MEMBER_COUNT -> {
          var tmCount = TEAM_MEMBERS.as("tm_count");
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(tmCount)
                      .where(tmCount.TEAM_ID.eq(TEAMS.ID).and(tmCount.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case ROLE ->
            condition =
                condition.and(
                    DSL.exists(
                        dsl.selectOne()
                            .from(TEAM_MEMBERS)
                            .where(
                                TEAM_MEMBERS
                                    .TEAM_ID
                                    .eq(TEAMS.ID)
                                    .and(TEAM_MEMBERS.DELETED_AT.isNull())
                                    .and(buildStrCondition(TEAM_MEMBERS.ROLE, c)))));
        case IS_OWNER ->
            condition =
                condition.and(
                    DSL.exists(
                        dsl.selectOne()
                            .from(TEAM_MEMBERS)
                            .where(
                                TEAM_MEMBERS
                                    .TEAM_ID
                                    .eq(TEAMS.ID)
                                    .and(TEAM_MEMBERS.DELETED_AT.isNull())
                                    .and(buildBoolCondition(TEAM_MEMBERS.IS_OWNER, c)))));
        case CONTRACT_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(CONTRACTS)
                      .where(CONTRACTS.TEAM_ID.eq(TEAMS.ID).and(CONTRACTS.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case CONTACT_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(CONTACTS)
                      .where(CONTACTS.TEAM_ID.eq(TEAMS.ID).and(CONTACTS.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case PHOTO_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(PHOTOS)
                      .where(PHOTOS.TEAM_ID.eq(TEAMS.ID).and(PHOTOS.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case DOCUMENT_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(DOCUMENTS)
                      .where(DOCUMENTS.TEAM_ID.eq(TEAMS.ID).and(DOCUMENTS.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case EXPENSE_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(EXPENSES)
                      .where(EXPENSES.TEAM_ID.eq(TEAMS.ID).and(EXPENSES.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case PAYMENT_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(PAYMENTS)
                      .where(PAYMENTS.TEAM_ID.eq(TEAMS.ID).and(PAYMENTS.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case CALENDAR_FEED_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(CALENDAR_FEEDS)
                      .where(
                          CALENDAR_FEEDS
                              .TEAM_ID
                              .eq(TEAMS.ID)
                              .and(CALENDAR_FEEDS.DELETED_AT.isNull())));
          condition = condition.and(buildNumCondition(cnt, c));
        }
        case EMAIL, EMAIL_VERIFIED -> {
          condition = condition.and(buildUserExistsCondition(c));
        }
        case IS_TEAM_SCOPE -> condition = condition.and(buildScopeCondition(true, c));
        case IS_USER_SCOPE -> condition = condition.and(buildScopeCondition(false, c));
        case TEAM_NAME -> condition = condition.and(buildStrCondition(TEAMS.NAME, c));
        case TEAM_ADMIN_EMAIL -> condition = condition.and(buildTeamMemberEmailCondition(c, true));
        case TEAM_OWNER_EMAIL -> condition = condition.and(buildTeamMemberEmailCondition(c, false));
        case TEAM_CURRENCY ->
            condition =
                condition.and(buildTeamPreferencesCondition(TEAM_PREFERENCES.DEFAULT_CURRENCY, c));
        case TEAM_DEFAULT_COUNTRY ->
            condition =
                condition.and(
                    buildTeamPreferencesCondition(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE, c));
        case TEAM_TIMEZONE ->
            condition = condition.and(buildTeamPreferencesCondition(TEAM_PREFERENCES.TIMEZONE, c));
      }
    }

    return dsl.select(TEAMS.ID, TEAMS.IDENTIFIER, TEAMS.NAME, TEAMS.DEMO, TEAMS.CREATED_AT)
        .from(TEAMS)
        .where(condition)
        .orderBy(TEAMS.NAME)
        .limit(500)
        .fetch(
            r ->
                new MatchingTeam(
                    r.get(TEAMS.ID),
                    r.get(TEAMS.IDENTIFIER),
                    r.get(TEAMS.NAME),
                    r.get(TEAMS.DEMO),
                    r.get(TEAMS.CREATED_AT).toInstant(UTC)));
  }

  /** Find distinct users matching all segment conditions. Limited to 500. */
  public List<MatchingUser> findMatchingUsers(SegmentDefinition segment) {
    if (segment.getConditions().isEmpty()) {
      return List.of();
    }

    Condition teamCond = TEAMS.DELETED_AT.isNull();
    Condition memberCond = TEAM_MEMBERS.DELETED_AT.isNull();
    Condition userCond = USERS.DELETED_AT.isNull();

    for (SegmentCondition c : segment.getConditions()) {
      switch (c.getAttribute()) {
        case IS_DEMO -> teamCond = teamCond.and(buildBoolCondition(TEAMS.DEMO, c));
        case TEAM_AGE_DAYS -> {
          Field<Long> ageDays =
              DSL.field("EXTRACT(EPOCH FROM (now() - {0})) / 86400", Long.class, TEAMS.CREATED_AT);
          teamCond = teamCond.and(buildNumCondition(ageDays, c));
        }
        case PROPERTY_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(PROPERTIES)
                      .where(PROPERTIES.TEAM_ID.eq(TEAMS.ID).and(PROPERTIES.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case MEMBER_COUNT -> {
          var tmCount = TEAM_MEMBERS.as("tm_count");
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(tmCount)
                      .where(tmCount.TEAM_ID.eq(TEAMS.ID).and(tmCount.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case CONTRACT_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(CONTRACTS)
                      .where(CONTRACTS.TEAM_ID.eq(TEAMS.ID).and(CONTRACTS.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case CONTACT_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(CONTACTS)
                      .where(CONTACTS.TEAM_ID.eq(TEAMS.ID).and(CONTACTS.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case PHOTO_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(PHOTOS)
                      .where(PHOTOS.TEAM_ID.eq(TEAMS.ID).and(PHOTOS.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case DOCUMENT_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(DOCUMENTS)
                      .where(DOCUMENTS.TEAM_ID.eq(TEAMS.ID).and(DOCUMENTS.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case EXPENSE_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(EXPENSES)
                      .where(EXPENSES.TEAM_ID.eq(TEAMS.ID).and(EXPENSES.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case PAYMENT_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(PAYMENTS)
                      .where(PAYMENTS.TEAM_ID.eq(TEAMS.ID).and(PAYMENTS.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case CALENDAR_FEED_COUNT -> {
          Field<Integer> cnt =
              DSL.field(
                  dsl.selectCount()
                      .from(CALENDAR_FEEDS)
                      .where(
                          CALENDAR_FEEDS
                              .TEAM_ID
                              .eq(TEAMS.ID)
                              .and(CALENDAR_FEEDS.DELETED_AT.isNull())));
          teamCond = teamCond.and(buildNumCondition(cnt, c));
        }
        case ROLE -> memberCond = memberCond.and(buildStrCondition(TEAM_MEMBERS.ROLE, c));
        case IS_OWNER -> memberCond = memberCond.and(buildBoolCondition(TEAM_MEMBERS.IS_OWNER, c));
        case EMAIL -> userCond = userCond.and(buildStrCondition(USERS.EMAIL, c));
        case EMAIL_VERIFIED -> {
          if ("true".equals(c.getValue())) {
            userCond = userCond.and(USERS.EMAIL_VERIFIED_AT.isNotNull());
          } else {
            userCond = userCond.and(USERS.EMAIL_VERIFIED_AT.isNull());
          }
        }
        case IS_TEAM_SCOPE -> teamCond = teamCond.and(buildScopeCondition(false, c));
        case IS_USER_SCOPE -> teamCond = teamCond.and(buildScopeCondition(true, c));
        case TEAM_NAME -> teamCond = teamCond.and(buildStrCondition(TEAMS.NAME, c));
        case TEAM_ADMIN_EMAIL -> teamCond = teamCond.and(buildTeamMemberEmailCondition(c, true));
        case TEAM_OWNER_EMAIL -> teamCond = teamCond.and(buildTeamMemberEmailCondition(c, false));
        case TEAM_CURRENCY ->
            teamCond =
                teamCond.and(buildTeamPreferencesCondition(TEAM_PREFERENCES.DEFAULT_CURRENCY, c));
        case TEAM_DEFAULT_COUNTRY ->
            teamCond =
                teamCond.and(
                    buildTeamPreferencesCondition(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE, c));
        case TEAM_TIMEZONE ->
            teamCond = teamCond.and(buildTeamPreferencesCondition(TEAM_PREFERENCES.TIMEZONE, c));
      }
    }

    return dsl.select(
            USERS.ID,
            USERS.IDENTIFIER,
            USERS.EMAIL,
            USERS.FIRST_NAME,
            USERS.LAST_NAME,
            TEAM_MEMBERS.ROLE,
            TEAMS.IDENTIFIER,
            TEAMS.NAME)
        .from(TEAMS)
        .join(TEAM_MEMBERS)
        .on(TEAM_MEMBERS.TEAM_ID.eq(TEAMS.ID).and(memberCond))
        .join(USERS)
        .on(USERS.ID.eq(TEAM_MEMBERS.USER_ID).and(userCond))
        .where(teamCond)
        .orderBy(USERS.EMAIL)
        .limit(500)
        .fetch(
            r ->
                new MatchingUser(
                    r.get(USERS.ID),
                    r.get(USERS.IDENTIFIER),
                    r.get(USERS.EMAIL),
                    r.get(USERS.FIRST_NAME),
                    r.get(USERS.LAST_NAME),
                    r.get(TEAM_MEMBERS.ROLE),
                    r.get(TEAMS.IDENTIFIER),
                    r.get(TEAMS.NAME)));
  }

  /** Builds a static true/false condition for scope attributes (is_team, is_user). */
  private Condition buildScopeCondition(boolean scopeValue, SegmentCondition c) {
    boolean expected = "true".equals(c.getValue());
    boolean matches =
        switch (c.getOperator()) {
          case EQ -> scopeValue == expected;
          case NEQ -> scopeValue != expected;
          default -> true;
        };
    return matches ? DSL.trueCondition() : DSL.falseCondition();
  }

  // --- Condition builders ---

  private Condition buildBoolCondition(Field<Boolean> field, SegmentCondition c) {
    boolean expected = "true".equals(c.getValue());
    return switch (c.getOperator()) {
      case EQ -> expected ? field.isTrue() : field.isFalse();
      case NEQ -> expected ? field.isFalse() : field.isTrue();
      default -> DSL.trueCondition();
    };
  }

  private Condition buildStrCondition(Field<String> field, SegmentCondition c) {
    return switch (c.getOperator()) {
      case EQ -> field.eq(c.getValue());
      case NEQ -> field.ne(c.getValue());
      case IN -> field.in(List.of(c.getValue().split(",")));
      case NOT_IN -> field.notIn(List.of(c.getValue().split(",")));
      case CONTAINS -> field.contains(c.getValue());
      case NOT_CONTAINS -> field.notContains(c.getValue());
      case STARTS_WITH -> field.startsWith(c.getValue());
      case ENDS_WITH -> field.endsWith(c.getValue());
      case REGEX -> DSL.condition("{0} ~ {1}", field, DSL.val(c.getValue()));
      default -> DSL.trueCondition();
    };
  }

  private Condition buildNumCondition(Field<?> field, SegmentCondition c) {
    long val = Long.parseLong(c.getValue());
    Field<Long> numField = field.cast(Long.class);
    return switch (c.getOperator()) {
      case EQ -> numField.eq(val);
      case NEQ -> numField.ne(val);
      case GT -> numField.gt(val);
      case GTE -> numField.ge(val);
      case LT -> numField.lt(val);
      case LTE -> numField.le(val);
      default -> DSL.trueCondition();
    };
  }

  /** Builds a team-level condition for an attribute whose value lives in TEAM_PREFERENCES. */
  private Condition buildTeamPreferencesCondition(Field<String> prefField, SegmentCondition c) {
    return DSL.exists(
        dsl.selectOne()
            .from(TEAM_PREFERENCES)
            .where(TEAM_PREFERENCES.TEAM_ID.eq(TEAMS.ID).and(buildStrCondition(prefField, c))));
  }

  /**
   * Builds an EXISTS condition matching a team member with the given role/owner-flag whose user
   * email matches the string condition. Team passes if ANY such member matches.
   */
  private Condition buildTeamMemberEmailCondition(SegmentCondition c, boolean adminOnly) {
    Condition memberFilter =
        TEAM_MEMBERS
            .TEAM_ID
            .eq(TEAMS.ID)
            .and(TEAM_MEMBERS.DELETED_AT.isNull())
            .and(USERS.ID.eq(TEAM_MEMBERS.USER_ID))
            .and(USERS.DELETED_AT.isNull())
            .and(buildStrCondition(USERS.EMAIL, c));
    memberFilter =
        adminOnly
            ? memberFilter.and(TEAM_MEMBERS.ROLE.eq("TEAM_ADMIN"))
            : memberFilter.and(TEAM_MEMBERS.IS_OWNER.isTrue());
    return DSL.exists(
        dsl.selectOne()
            .from(TEAM_MEMBERS)
            .join(USERS)
            .on(USERS.ID.eq(TEAM_MEMBERS.USER_ID))
            .where(memberFilter));
  }

  private Condition buildUserExistsCondition(SegmentCondition c) {
    Condition innerCond =
        TEAM_MEMBERS
            .TEAM_ID
            .eq(TEAMS.ID)
            .and(TEAM_MEMBERS.DELETED_AT.isNull())
            .and(USERS.ID.eq(TEAM_MEMBERS.USER_ID))
            .and(USERS.DELETED_AT.isNull());

    switch (c.getAttribute()) {
      case EMAIL -> innerCond = innerCond.and(buildStrCondition(USERS.EMAIL, c));
      case EMAIL_VERIFIED -> {
        if ("true".equals(c.getValue())) {
          innerCond = innerCond.and(USERS.EMAIL_VERIFIED_AT.isNotNull());
        } else {
          innerCond = innerCond.and(USERS.EMAIL_VERIFIED_AT.isNull());
        }
      }
      default -> {
        // Not a user-level attribute — no-op
      }
    }

    return DSL.exists(
        dsl.selectOne()
            .from(TEAM_MEMBERS)
            .join(USERS)
            .on(USERS.ID.eq(TEAM_MEMBERS.USER_ID))
            .where(innerCond));
  }

  private SegmentDefinition toSegmentDomain(SegmentsRecord r, List<SegmentCondition> conditions) {
    return SegmentDefinition.builder()
        .id(r.getId())
        .key(r.getKey())
        .name(r.getName())
        .description(Optional.ofNullable(r.getDescription()))
        .priority(r.getPriority())
        .conditions(List.copyOf(conditions))
        .createdAt(r.getCreatedAt().toInstant(UTC))
        .updatedAt(r.getUpdatedAt().toInstant(UTC))
        .createdBy(Optional.ofNullable(r.getCreatedBy()))
        .updatedBy(Optional.ofNullable(r.getUpdatedBy()))
        .deletedAt(Optional.ofNullable(r.getDeletedAt()).map(dt -> dt.toInstant(UTC)))
        .build();
  }

  private SegmentCondition toConditionDomain(SegmentConditionsRecord r) {
    return SegmentCondition.builder()
        .id(r.getId())
        .segmentId(r.getSegmentId())
        .attribute(SegmentAttribute.fromDbValue(r.getAttribute()))
        .operator(SegmentOperator.fromDbValue(r.getOperator()))
        .value(r.getValue())
        .createdAt(r.getCreatedAt().toInstant(UTC))
        .updatedAt(r.getUpdatedAt().toInstant(UTC))
        .createdBy(Optional.ofNullable(r.getCreatedBy()))
        .updatedBy(Optional.ofNullable(r.getUpdatedBy()))
        .build();
  }
}
