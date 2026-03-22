package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.CONTACT_TAGS;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.ContactTag;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ContactTagRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public void addTag(UUID contactId, UUID teamId, ContactTag tag, UUID createdBy) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.insertInto(CONTACT_TAGS)
        .set(CONTACT_TAGS.ID, UUID.randomUUID())
        .set(CONTACT_TAGS.TEAM_ID, teamId)
        .set(CONTACT_TAGS.CONTACT_ID, contactId)
        .set(CONTACT_TAGS.TAG, tag.name())
        .set(CONTACT_TAGS.CREATED_AT, now)
        .set(CONTACT_TAGS.CREATED_BY, createdBy)
        .execute();
  }

  public void removeTag(UUID contactId, UUID teamId, ContactTag tag) {
    dsl.deleteFrom(CONTACT_TAGS)
        .where(
            CONTACT_TAGS
                .CONTACT_ID
                .eq(contactId)
                .and(CONTACT_TAGS.TEAM_ID.eq(teamId))
                .and(CONTACT_TAGS.TAG.eq(tag.name())))
        .execute();
  }

  public List<ContactTag> findByContactId(UUID contactId, UUID teamId) {
    return List.copyOf(
        dsl.select(CONTACT_TAGS.TAG)
            .from(CONTACT_TAGS)
            .where(CONTACT_TAGS.CONTACT_ID.eq(contactId).and(CONTACT_TAGS.TEAM_ID.eq(teamId)))
            .fetch(record -> ContactTag.valueOf(record.get(CONTACT_TAGS.TAG))));
  }

  public Map<UUID, List<ContactTag>> findByContactIdsGrouped(
      Collection<UUID> contactIds, UUID teamId) {
    if (contactIds.isEmpty()) {
      return Map.of();
    }
    return dsl
        .select(CONTACT_TAGS.CONTACT_ID, CONTACT_TAGS.TAG)
        .from(CONTACT_TAGS)
        .where(CONTACT_TAGS.CONTACT_ID.in(contactIds).and(CONTACT_TAGS.TEAM_ID.eq(teamId)))
        .fetch()
        .stream()
        .collect(
            Collectors.groupingBy(
                record -> record.get(CONTACT_TAGS.CONTACT_ID),
                Collectors.mapping(
                    record -> ContactTag.valueOf(record.get(CONTACT_TAGS.TAG)),
                    Collectors.toUnmodifiableList())));
  }

  public void deleteByContactId(UUID contactId, UUID teamId) {
    dsl.deleteFrom(CONTACT_TAGS)
        .where(CONTACT_TAGS.CONTACT_ID.eq(contactId).and(CONTACT_TAGS.TEAM_ID.eq(teamId)))
        .execute();
  }
}
