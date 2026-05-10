package com.buurman.service.demo;

import static com.buurman.jooq.generated.Tables.CONTACT_RELATIONSHIPS;
import static com.buurman.util.SidGenerator.newContactRelationshipId;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.ContactType;
import com.buurman.domain.RelationshipType;
import com.buurman.domain.Sid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class DemoContactRelationshipGenerator {

  private final DSLContext dsl;
  private final Clock clock;
  private final Random random = new Random(99);

  // Relationship notes templates per type
  private static final String[] GUARANTOR_NOTES = {
    "Acts as guarantor for the lease agreement.",
    "Personal guarantor — income verified.",
    "Guarantor letter received and filed.",
  };

  private static final String[] FAMILY_NOTES = {
    "Brother and sister.",
    "Parent and child — same household.",
    "Siblings sharing the same property.",
    null,
  };

  private static final String[] PARTNER_NOTES = {
    "Registered partners — co-tenants.", "Married couple on the same lease.", null,
  };

  private static final String[] WORKS_FOR_NOTES = {
    "Employee listed as emergency contact.", "Employed by the company contact.", null,
  };

  private static final String[] CONTACT_PERSON_NOTES = {
    "Primary contact person for the company.", "Emergency contact and day-to-day liaison.", null,
  };

  private static final String[] OTHER_NOTES = {
    "Known associate.", "Referred by this contact.", null,
  };

  public void generate(DemoDataContext ctx) {
    LocalDateTime now = LocalDateTime.now(clock);
    int totalRelationships = 0;

    for (var teamEntry : ctx.getTeamIds().entrySet()) {
      String teamKey = teamEntry.getKey();
      UUID teamId = teamEntry.getValue();
      UUID createdBy = ctx.getAdminUserForTeam(teamKey).orElse(null);

      List<UUID> allContacts = ctx.getContactIdsByTeam().getOrDefault(teamId, List.of());
      if (allContacts.size() < 2) {
        continue;
      }

      // Separate individual contacts from business contacts for realistic pairings
      List<UUID> individuals = new ArrayList<>();
      List<UUID> businesses = new ArrayList<>();
      for (UUID contactId : allContacts) {
        if (ctx.getContactType(contactId) == ContactType.INDIVIDUAL) {
          individuals.add(contactId);
        } else {
          businesses.add(contactId);
        }
      }

      // Shuffle for varied pairings
      Collections.shuffle(individuals, random);
      Collections.shuffle(businesses, random);

      // Create ~8–12 relationships per team
      int targetRelationships = random.nextInt(8, 13);
      int created = 0;

      List<Object[]> records = new ArrayList<>();

      // 1. GUARANTOR_FOR: individual guarantees another individual (up to 3)
      for (int i = 0; i + 1 < individuals.size() && created < 3; i += 2) {
        addRelationship(
            records,
            teamId,
            individuals.get(i),
            individuals.get(i + 1),
            RelationshipType.GUARANTOR_FOR,
            randomNullable(GUARANTOR_NOTES),
            now,
            createdBy);
        created++;
        totalRelationships++;
      }

      // 2. FAMILY_OF: pairs of individuals (up to 3)
      int familyStart = Math.min(6, individuals.size());
      for (int i = familyStart; i + 1 < individuals.size() && created < 6; i += 2) {
        addRelationship(
            records,
            teamId,
            individuals.get(i),
            individuals.get(i + 1),
            RelationshipType.FAMILY_OF,
            randomNullable(FAMILY_NOTES),
            now,
            createdBy);
        created++;
        totalRelationships++;
      }

      // 3. PARTNER_OF: one pair of individuals
      if (individuals.size() >= 2) {
        int partnerIdx = Math.min(10, individuals.size() - 2);
        addRelationship(
            records,
            teamId,
            individuals.get(partnerIdx),
            individuals.get(partnerIdx + 1),
            RelationshipType.PARTNER_OF,
            randomNullable(PARTNER_NOTES),
            now,
            createdBy);
        created++;
        totalRelationships++;
      }

      // 4. WORKS_FOR: individual works for a business contact (up to 3)
      for (int i = 0; i < businesses.size() && i < 3 && i < individuals.size(); i++) {
        addRelationship(
            records,
            teamId,
            individuals.get(individuals.size() - 1 - i),
            businesses.get(i),
            RelationshipType.WORKS_FOR,
            randomNullable(WORKS_FOR_NOTES),
            now,
            createdBy);
        created++;
        totalRelationships++;
      }

      // 5. CONTACT_PERSON_FOR: individual is contact person for a business (up to 2)
      for (int i = 0; i < Math.min(2, businesses.size()) && created < targetRelationships; i++) {
        int indivIdx = Math.min(i + 5, individuals.size() - 1);
        if (indivIdx >= 0) {
          addRelationship(
              records,
              teamId,
              individuals.get(indivIdx),
              businesses.get(i),
              RelationshipType.CONTACT_PERSON_FOR,
              randomNullable(CONTACT_PERSON_NOTES),
              now,
              createdBy);
          created++;
          totalRelationships++;
        }
      }

      // 6. OTHER: one catch-all
      if (created < targetRelationships && individuals.size() >= 4) {
        addRelationship(
            records,
            teamId,
            individuals.get(2),
            individuals.get(3),
            RelationshipType.OTHER,
            randomNullable(OTHER_NOTES),
            now,
            createdBy);
        totalRelationships++;
      }

      if (!records.isEmpty()) {
        var insert =
            dsl.insertInto(CONTACT_RELATIONSHIPS)
                .columns(
                    CONTACT_RELATIONSHIPS.ID,
                    CONTACT_RELATIONSHIPS.IDENTIFIER,
                    CONTACT_RELATIONSHIPS.TEAM_ID,
                    CONTACT_RELATIONSHIPS.SOURCE_CONTACT_ID,
                    CONTACT_RELATIONSHIPS.TARGET_CONTACT_ID,
                    CONTACT_RELATIONSHIPS.RELATIONSHIP_TYPE,
                    CONTACT_RELATIONSHIPS.NOTES,
                    CONTACT_RELATIONSHIPS.CREATED_AT,
                    CONTACT_RELATIONSHIPS.UPDATED_AT,
                    CONTACT_RELATIONSHIPS.CREATED_BY,
                    CONTACT_RELATIONSHIPS.UPDATED_BY)
                .values(
                    (UUID) null,
                    (Sid) null,
                    (UUID) null,
                    (UUID) null,
                    (UUID) null,
                    (String) null,
                    (String) null,
                    (LocalDateTime) null,
                    (LocalDateTime) null,
                    (UUID) null,
                    (UUID) null);
        var batch = dsl.batch(insert);
        for (Object[] r : records) {
          batch = batch.bind(r);
        }
        batch.execute();
      }

      log.info("Created contact relationships for team {}", teamKey);
    }

    log.info("Created {} contact relationships total", totalRelationships);
  }

  private void addRelationship(
      List<Object[]> records,
      UUID teamId,
      UUID sourceId,
      UUID targetId,
      RelationshipType type,
      @Nullable String notes,
      LocalDateTime now,
      @Nullable UUID createdBy) {
    LocalDateTime createdAt = now.minusDays(random.nextInt(1, 365));
    records.add(
        new Object[] {
          UUID.randomUUID(),
          newContactRelationshipId(),
          teamId,
          sourceId,
          targetId,
          type.name(),
          notes,
          createdAt,
          createdAt,
          createdBy,
          createdBy
        });
  }

  private @Nullable String randomNullable(String[] options) {
    return options[random.nextInt(options.length)];
  }
}
