package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.CONTACT_RELATIONSHIPS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.ContactRelationship;
import com.buurman.domain.RelationshipType;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.ContactRelationshipRecordMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ContactRelationshipRepository {

  private final DSLContext dsl;
  private final ContactRelationshipRecordMapper mapper;
  private final Clock clock;

  public ContactRelationship save(ContactRelationship relationship) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (relationship.getId() == null) {
      // INSERT
      UUID newId = UUID.randomUUID();

      dsl.insertInto(CONTACT_RELATIONSHIPS)
          .set(CONTACT_RELATIONSHIPS.ID, newId)
          .set(CONTACT_RELATIONSHIPS.IDENTIFIER, relationship.getIdentifier().orElseThrow())
          .set(CONTACT_RELATIONSHIPS.TEAM_ID, relationship.getTeamId())
          .set(CONTACT_RELATIONSHIPS.SOURCE_CONTACT_ID, relationship.getSourceContactId())
          .set(CONTACT_RELATIONSHIPS.TARGET_CONTACT_ID, relationship.getTargetContactId())
          .set(CONTACT_RELATIONSHIPS.RELATIONSHIP_TYPE, relationship.getRelationshipType().name())
          .set(CONTACT_RELATIONSHIPS.NOTES, relationship.getNotes().orElse(null))
          .set(CONTACT_RELATIONSHIPS.CREATED_AT, now)
          .set(CONTACT_RELATIONSHIPS.UPDATED_AT, now)
          .set(CONTACT_RELATIONSHIPS.CREATED_BY, relationship.getCreatedBy())
          .set(CONTACT_RELATIONSHIPS.UPDATED_BY, relationship.getUpdatedBy())
          .execute();

      relationship.setId(newId);
      relationship.setCreatedAt(now.toInstant(UTC));
      relationship.setUpdatedAt(now.toInstant(UTC));
    } else {
      // UPDATE
      dsl.update(CONTACT_RELATIONSHIPS)
          .set(CONTACT_RELATIONSHIPS.RELATIONSHIP_TYPE, relationship.getRelationshipType().name())
          .set(CONTACT_RELATIONSHIPS.NOTES, relationship.getNotes().orElse(null))
          .set(CONTACT_RELATIONSHIPS.UPDATED_AT, now)
          .set(CONTACT_RELATIONSHIPS.UPDATED_BY, relationship.getUpdatedBy())
          .where(
              CONTACT_RELATIONSHIPS
                  .ID
                  .eq(relationship.getId())
                  .and(CONTACT_RELATIONSHIPS.TEAM_ID.eq(relationship.getTeamId())))
          .execute();

      relationship.setUpdatedAt(now.toInstant(UTC));
    }

    return relationship;
  }

  public List<ContactRelationship> findByContactIdAndTeamId(UUID contactId, UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(CONTACT_RELATIONSHIPS)
            .where(
                CONTACT_RELATIONSHIPS
                    .SOURCE_CONTACT_ID
                    .eq(contactId)
                    .or(CONTACT_RELATIONSHIPS.TARGET_CONTACT_ID.eq(contactId)))
            .and(CONTACT_RELATIONSHIPS.TEAM_ID.eq(teamId))
            .and(CONTACT_RELATIONSHIPS.DELETED_AT.isNull())
            .orderBy(CONTACT_RELATIONSHIPS.CREATED_AT.desc())
            .fetch()
            .map(mapper::toDomain));
  }

  public Optional<ContactRelationship> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(CONTACT_RELATIONSHIPS)
        .where(
            CONTACT_RELATIONSHIPS
                .IDENTIFIER
                .eq(identifier)
                .and(CONTACT_RELATIONSHIPS.TEAM_ID.eq(teamId))
                .and(CONTACT_RELATIONSHIPS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public ContactRelationship getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Contact relationship not found"));
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(CONTACT_RELATIONSHIPS)
        .set(CONTACT_RELATIONSHIPS.DELETED_AT, now)
        .where(CONTACT_RELATIONSHIPS.ID.eq(id).and(CONTACT_RELATIONSHIPS.TEAM_ID.eq(teamId)))
        .execute();
  }

  public boolean existsByPairAndType(
      UUID teamId, UUID sourceId, UUID targetId, RelationshipType type) {
    return dsl.fetchExists(
        dsl.selectFrom(CONTACT_RELATIONSHIPS)
            .where(
                CONTACT_RELATIONSHIPS
                    .TEAM_ID
                    .eq(teamId)
                    .and(CONTACT_RELATIONSHIPS.SOURCE_CONTACT_ID.eq(sourceId))
                    .and(CONTACT_RELATIONSHIPS.TARGET_CONTACT_ID.eq(targetId))
                    .and(CONTACT_RELATIONSHIPS.RELATIONSHIP_TYPE.eq(type.name()))
                    .and(CONTACT_RELATIONSHIPS.DELETED_AT.isNull())));
  }

  public void softDeleteByContactId(UUID contactId, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(CONTACT_RELATIONSHIPS)
        .set(CONTACT_RELATIONSHIPS.DELETED_AT, now)
        .where(
            CONTACT_RELATIONSHIPS
                .SOURCE_CONTACT_ID
                .eq(contactId)
                .or(CONTACT_RELATIONSHIPS.TARGET_CONTACT_ID.eq(contactId)))
        .and(CONTACT_RELATIONSHIPS.TEAM_ID.eq(teamId))
        .execute();
  }
}
